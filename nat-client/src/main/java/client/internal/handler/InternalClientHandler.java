package client.internal.handler;

import core.netty.group.ClientChannelGroup;
import client.internal.handler.processor.constant.ProcessorEnum;
import client.internal.InternalNettyClient;
import core.entity.Frame;
import core.netty.group.channel.message.MessageContext;
import core.netty.handler.processor.ProcessorManager;
import core.properties.cache.PropertiesCache;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @Author wneck130@gmail.com
 * @function 业务处理handler，所有协议命令在本类中处理
 */
@Slf4j
public class InternalClientHandler extends SimpleChannelInboundHandler<Frame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        byte cmd = msg.getCmd();
        ProcessorManager.getInstance(ProcessorEnum.getClassByCmd(cmd)).request(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ClientChannelGroup.internalGroup.size() > PropertiesCache.getInstance().getInt(InternalNettyClient.INIT_NUM)) {
            ctx.close();
            return;
        }
        MessageContext messageContext = MessageContext.getInstance();
        //将所有指令的处理器都指派给messageContext作为监听器
        Arrays.stream(ProcessorEnum.values()).forEach(processorEnum -> {
            messageContext.addResponseListener(ProcessorManager.getInstance(processorEnum.getClazz()));
        });
        ClientChannelGroup.addInternal(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.removeInternal(ctx.channel());
        log.debug("[internalClient：{}]断开连接", ctx.channel().id().asShortText());
        InternalNettyClient internalClient = new InternalNettyClient();
        if (ClientChannelGroup.internalGroup.size() < PropertiesCache.getInstance().getInt(InternalNettyClient.INIT_NUM)) {
            internalClient.start();
        }

    }

    /**
     * 通道异常触发
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ClientInternal[{}]发生异常：{}", ctx.channel().id(), cause.getStackTrace());
        ctx.close();
    }
}
