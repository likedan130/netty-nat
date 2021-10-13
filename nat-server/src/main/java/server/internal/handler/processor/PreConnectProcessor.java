package server.internal.handler.processor;

import core.netty.group.channel.message.ResponseEvent;
import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.handler.processor.Processor;
import core.netty.group.ServerChannelGroup;
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
 * @author wneck130@gmail.com
 * @Description: 预创建连接命令处理器（cmd:0x03），命令由proxyServer发起请求，proxyClient回复响应
 * @date 2021/9/27
 */
@Slf4j
public class PreConnectProcessor implements Processor {

    /**
     * preConnect数据帧组装，服务端处理的是响应命令
     * @param in netty获取的TCP数据流
     * @return
     * @throws Exception
     */
    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            Frame frame = new Frame().quickHead(in);
            byte result = in.readByte();
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>(1);
            dataMap.put("result", result);
            frame.setData(dataMap);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    /**
     * 处理预创建连接的响应消息
     * @param responseEvent
     */
    @Override
    public void response(ResponseEvent responseEvent) {
        //处理响应信息
        byte result = (byte)responseEvent.getResponse().getData().get("result");
        if (result == FrameConstant.RESULT_SUCCESS) {
            //成功后缓存配对关系
            ServerChannelGroup.addChannelPair(responseEvent.getResponse().getReq(),
                    responseEvent.getResponse().getRes());
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

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.PRE_CONNECT.getCmd(), cmd);
    }
}
