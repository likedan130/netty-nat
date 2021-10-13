package server.internal.handler.processor;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.group.ServerChannelGroup;
import core.netty.group.channel.message.ResponseEvent;
import core.netty.handler.processor.Processor;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @Author wneck130@gmail.com
 * @function 数据下发处理器（cmd：0xEE），由proxyServer发起请求，proxyClient回复响应
 */
@Slf4j
public class DownStreamProcessor implements Processor {

    /**
     * downStream数据帧处理，在服务端，处理的是downStream命令的响应
     * @param in netty获取的TCP数据流
     * @return
     * @throws Exception
     */
    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            Frame frame = new Frame().quickHead(in);
            //解析协议data部分
            byte result = in.readByte();
            //生成数据帧
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("result", result);
            frame.setData(dataMap);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    @Override
    public void response(ResponseEvent responseEvent) {
        try {
            byte result = (byte)responseEvent.getResponse().getData().get("result");
            if (result != FrameConstant.RESULT_SUCCESS) {
                log.error("来自{}的消息收到客户端处理失败响应，尝试重发!!!", responseEvent.getChannelId());
                Channel internalChannel = ServerChannelGroup.forkChannel(responseEvent.getChannelId());
                internalChannel.writeAndFlush(responseEvent.getRequest());
            }
        } catch (Exception e) {
            log.error("channel[{}]处理响应指令CMD:{},SERIAL:{}重试失败", responseEvent.getChannelId(),
                    responseEvent.getRequest().getCmd(), responseEvent.getRequest().getSerial());
        }
    }

    @Override
    public void timeout(ResponseEvent responseEvent) {
        try {
            //超时后默认进行一次重试
            Channel internalChannel = ServerChannelGroup.forkChannel(responseEvent.getChannelId());
            internalChannel.writeAndFlush(responseEvent.getRequest());
        } catch (Exception e) {
            log.error("channel[{}]处理超时指令CMD:{},SERIAL:{}重试失败", responseEvent.getChannelId(),
                    responseEvent.getRequest().getCmd(), responseEvent.getRequest().getSerial());
        }
    }

    /**
     * processor对应的命令字适配
     * @param cmd
     * @return
     */
    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.DOWN_STREAM.getCmd(), cmd);
    }
}
