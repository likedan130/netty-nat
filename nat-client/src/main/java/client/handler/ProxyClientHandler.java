package client.handler;

import client.group.ClientChannelGroup;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Logger log = LoggerFactory.getLogger(ProxyClientHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            Channel internalChannnel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
            if (internalChannnel != null) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                internalChannnel.writeAndFlush(byteBuf).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        log.info("向internalServer发送消息成功："+future.isSuccess());
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
        super.channelInactive(ctx);
    }
}
