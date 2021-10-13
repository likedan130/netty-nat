package server.internal.handler.processor;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.handler.processor.Processor;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @Author wneck130@gmail.com
 * @Function 心跳命令处理器（cmd:0x02），命令由internalClient发起请求，internalServer回复响应
 */
@Slf4j
public class HeartbeatProcessor implements Processor {

    /**
     * heartbeat数据帧处理
     * @param in netty获取的TCP数据流
     * @return
     * @throws Exception
     */
    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            return new Frame().quickHead(in);
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    /**
     * 处理heartbeat业务
     * @param ctx netty channel上下文
     * @param msg 解析成Frame结构的TCP请求数据帧
     * @throws Exception
     */
    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //保持连接活性，回复心跳
        Frame response = new Frame();
        response.setPv(FrameConstant.RES_PV);
        response.setSerial(msg.getSerial());
        response.setReq(msg.getReq());
        response.setRes(ctx.channel().id().asShortText());
        response.setCmd(msg.getCmd());
        ctx.writeAndFlush(response);
    }

    /**
     * processor对应的命令字适配
     * @param cmd
     * @return
     */
    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.HEARTBEAT.getCmd(), cmd);
    }
}
