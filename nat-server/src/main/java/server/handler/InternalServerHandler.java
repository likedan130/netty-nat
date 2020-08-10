package server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.group.ServerChannelGroup;

public class InternalServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(InternalServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //通过内部的internalChannel收到响应详细，转发到代理服务的请求者
        ChannelId channelId = ctx.channel().id();
        if (ServerChannelGroup.channelPairExist(channelId)) {
            //已经存在配对的连接，直接发送，响应时无配对数据可能是channel断开连接导致的，结束消息传递
            Channel proxyChannel = ServerChannelGroup.getProxyByInternal(channelId);
            //不为空且处于已连接活跃状态
            if (proxyChannel != null && proxyChannel.isActive()) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                proxyChannel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.error("InternalServer send data to proxyServer exception occur: ", future.cause());
                    }
                });
            }
        }else {
            Channel proxyChannel = ServerChannelGroup.getProxyByInternal(channelId);
            //不为空且处于已连接活跃状态
            if (proxyChannel != null && proxyChannel.isActive()) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                proxyChannel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.error("InternalServer send data to proxyServer exception occur: ", future.cause());
                    }
                });
            }
        }
    }

    /**
     * 内部通道建立缓存机制，所有可用通道进入空闲连接池，等待配对
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ServerChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    /**
     * 通道断开连接时回收已经配对的信息
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerChannelGroup.removeIdleInternalChannel(ctx.channel());
        ServerChannelGroup.removeInternalChannel(ctx.channel());
        Channel proxyChannel = ServerChannelGroup.getProxyByInternal(ctx.channel().id());
        ServerChannelGroup.removeChannelPair(ctx.channel().id(), proxyChannel.id());
        proxyChannel.close();
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
            logger.debug("############### -- 客户端 -- "+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
            ctx.fireExceptionCaught(cause);
            logger.debug("###############",cause);
        }
    }
}
