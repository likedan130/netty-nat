package client.handler;

import client.group.ClientChannelGroup;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        System.out.println("ProxyServer-"+ctx.channel().id()+"-收到消息："+format.format(new Date()));
        System.out.println("代理客户端channelRead0收到："+ ByteUtil.toHexString(BufUtil.getArray(msg)));
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            Channel internalChannnel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
            if (internalChannnel != null) {
//                System.out.println("匹配到internalChannel");
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                internalChannnel.writeAndFlush(byteBuf).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
//                        System.out.println("向internalChannel发送消息成功："+future.isSuccess());
                    }
                });
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("代理客户端channelActive收到："+ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
