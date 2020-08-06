package client.handler;

import client.group.ClientChannelGroup;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序内部连接客户端处理器
 */
public class InternalClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("InternalClientHandler:"+ctx.channel().id());
        ClientChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ChannelId channelId = ctx.channel().id();
        if (ClientChannelGroup.channelPairExist(channelId)) {
            //如果存在配对，直接转发消息
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(channelId);
            if(proxyChannel != null) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(message);
                proxyChannel.writeAndFlush(byteBuf);
            }else {
                sendMessage(msg,ctx.channel());
            }
        }else {
            sendMessage(msg,ctx.channel());
        }
    }
    private void sendMessage(ByteBuf msg,Channel channel) throws Exception{
        Channel proxyChannel = ClientChannelGroup.proxyNotExist(channel);
        byte[] message = new byte[msg.readableBytes()];
        msg.readBytes(message);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(message);
        proxyChannel.writeAndFlush(byteBuf);
    }
}
