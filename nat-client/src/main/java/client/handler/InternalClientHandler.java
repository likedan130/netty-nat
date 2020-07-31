package client.handler;

import client.group.ClientChannelGroup;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序内部连接客户端处理器
 */
@Slf4j
public class InternalClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("InternalClient-channelActive连接；"+ctx.channel().id());
        ClientChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        log.info("内部客户端channelRead0收到："+ctx.channel().id()+";"+ ByteUtil.toHexString(BufUtil.getArray(msg)));
        ChannelId channelId = ctx.channel().id();
        if (ClientChannelGroup.channelPairExist(channelId)) {
            //如果存在配对，直接转发消息
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(channelId);
            byte[] message = new byte[msg.readableBytes()];
            msg.readBytes(message);
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(message);
            proxyChannel.writeAndFlush(byteBuf).addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    log.info("向ProxyClient发送数据成功："+future.isSuccess());
                }
            });
        }else {
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(channelId);
            if (proxyChannel != null) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                proxyChannel.writeAndFlush(byteBuf).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        log.info("向向ProxyClient发送数据成功"+future.isSuccess());
                    }
                });
            }
        }
    }
}
