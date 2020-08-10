package client.handler;

import client.group.ClientChannelGroup;
import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序内部连接客户端处理器
 */
public class InternalClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(InternalClientHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        while (true){
            if(ClientChannelGroup.connectProxy == NumberConstant.ZERO){
                break;
            }
        }
        byte[] message = new byte[msg.readableBytes()];
        msg.readBytes(message);
        ChannelId channelId = ctx.channel().id();
        if (ClientChannelGroup.channelPairExist(channelId)) {
            //如果存在配对，直接转发消息
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(channelId);
            //不为空且处于已连接活跃状态
            if(proxyChannel != null && proxyChannel.isActive()) {
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                proxyChannel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.error("InternalClient send data to proxyClient exception occur: ", future.cause());
                    }
                });
            }else {
                sendMessage(message,ctx.channel());
            }
        }else {
            sendMessage(message,ctx.channel());
        }
    }
    private void sendMessage(byte[] message,Channel channel) throws Exception{
        Channel proxyChannel = ClientChannelGroup.proxyNotExist(channel);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(message);
        proxyChannel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.error("InternalClient send data to proxyClient exception occur: ", future.cause());
            }
        });
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
            logger.debug("客户端："+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
            ctx.fireExceptionCaught(cause);
            logger.debug("异常："+cause);
        }
    }
}
