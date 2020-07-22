package client.handler;

import client.group.ClientChannelGroup;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序内部连接客户端处理器
 */
public class InternalClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("内部客户端channelActive收到："+ctx);
        ClientChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println("内部客户端channelRead0收到："+ ByteUtil.toHexString(BufUtil.getArray(msg)));
        ChannelId channelId = ctx.channel().id();
        if (ClientChannelGroup.channelPairExist(channelId)) {
            //如果存在配对，直接转发消息
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(channelId);
            proxyChannel.writeAndFlush(msg);
        } else {
            //不存在配对时，先建立配对，再转发消息
            Channel proxyChannel = ClientChannelGroup.forkProxyChannel(ctx.channel());
            proxyChannel.writeAndFlush(msg);
        }
    }
}
