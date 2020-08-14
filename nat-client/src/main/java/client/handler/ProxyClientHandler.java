package client.handler;

import client.Client;
import client.group.ClientChannelGroup;
import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyClientHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //判断是否已经建立配对关系
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            //获取内部客户端
            Channel internalChannnel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
            //判断客户端channel是否活跃
            if (internalChannnel != null && internalChannnel.isActive()) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                internalChannnel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.error("send data to proxyClient exception occur: ", future.cause());
                    }
                });
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.removeProxyChannel(ctx.channel());
        Channel internalChannel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
        //如果是proxy连接断开，将之前配对的internal连接移除配对，并在2秒后回归空闲连接池，相当于有2秒的time_wait状态
        ClientChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
        Client.scheduledExecutor.schedule(
                () -> ClientChannelGroup.releaseInternalChannel(internalChannel),
                NumberConstant.TWO, TimeUnit.SECONDS);
    }

    /**
     * 通道异常触发
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if(!channel.isActive()){
            logger.debug("############### -- 被代理客户端 -- "+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
            ctx.fireExceptionCaught(cause);
            logger.debug("###############",cause);
        }
    }
}
