package client.internal.handler.processor;

import client.internal.handler.processor.constant.ProcessorEnum;
import core.entity.Frame;
import core.netty.handler.processor.Processor;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @Author wneck130@gmail.com
 * @function 心跳命令处理器（cmd:0x02），命令由internalClient发起请求，internalServer回复响应
 */
@Slf4j
public class HeartbeatProcessor implements Processor {

    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            Frame frame = new Frame().quickHead(in);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //TODO 预留心跳命令，对于长连接的保持有特殊需要时启用心跳
    }

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.HEARTBEAT.getCmd(), cmd);
    }
}
