package client.internal.handler.processor;

import client.internal.handler.processor.constant.ProcessorEnum;
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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @Author wneck130@gmail.com
 * @function 数据上传处理器（cmd:0xFF），命令由proxyClient发起请求，internalClient处理响应
 */
@Slf4j
public class UpStreamProcessor implements Processor {

    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            Frame frame = new Frame().quickHead(in);
            //解析协议data部分
            byte result = in.readByte();
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
            LinkedHashMap<String, Object> dataMap = responseEvent.getResponse().getData();
            byte result = (byte) dataMap.get("result");
            if (result == FrameConstant.RESULT_FAIL) {
                Channel internalChannel = ServerChannelGroup.forkChannel(responseEvent.getChannelId());
                internalChannel.writeAndFlush(responseEvent.getRequest());
            }
        } catch (Exception e) {
            log.error("channel[{}]处理响应CMD:{},SERIAL:{}重试失败", responseEvent.getChannelId(),
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
                    responseEvent.getRequest().getCmd(), responseEvent.getRequest().getSerial(), e);
        }
    }

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.UP_STREAM.getCmd(), cmd);
    }
}
