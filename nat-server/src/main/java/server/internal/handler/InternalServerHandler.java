package server.internal.handler;

import core.entity.Frame;
import core.netty.group.ServerChannelGroup;
import core.netty.handler.processor.ProcessorManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

/**
 * @Author wneck130@gmail.com
 * @function 业务处理handler，所有协议命令在本类中处理
 */
@Slf4j
public class InternalServerHandler extends SimpleChannelInboundHandler<Frame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        byte cmd = msg.getCmd();
        ProcessorManager.getInstance(ProcessorEnum.getClassByCmd(cmd)).request(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //internal channel创建时，维护internalGroup
        ServerChannelGroup.addInternal(ctx.channel());
        log.debug("channel[{}]进入internalChannel组!!!", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //internal channel销毁时，维护internalGroup
        ServerChannelGroup.removeInternal(ctx.channel());
    }

    /**
     * 通道异常触发
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ServerInternal[{}]发生异常：{}", ctx.channel().id(), cause.getStackTrace(), cause);
        ctx.close();
    }
}
