package server.handler;

import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.Server;
import server.group.ServerChannelGroup;
import java.util.concurrent.TimeUnit;

public class ProxyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println("代理服务channelRead0收到：" + ByteUtil.toHexString(BufUtil.getArray(msg)));
        //收到外部请求先找配对的内容连接
        Channel proxyChannel = ctx.channel();
        if (ServerChannelGroup.channelPairExist(proxyChannel.id())) {
            //已经存在配对，直接进行消息转发
            ServerChannelGroup.getInternalByProxy(proxyChannel.id()).writeAndFlush(msg);
        } else {
            //未配对的，先进行配对后，再消息转发
            ServerChannelGroup.forkChannel(ctx.channel());
            ServerChannelGroup.getInternalByProxy(proxyChannel.id()).writeAndFlush(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("代理服务channelActive收到："+ctx);
        //新的连接建立后先进行配对
        ServerChannelGroup.forkChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerChannelGroup.removeProxyChannel(ctx.channel());
        Channel internalChannel = ServerChannelGroup.getInternalByProxy(ctx.channel().id());
        //如果是proxy连接断开，将之前配对的internal连接移除配对，并在2秒后回归空闲连接池，相当于有2秒的time_wait状态
        ServerChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
        Server.scheduledExecutor.schedule(
                () -> ServerChannelGroup.releaseInternalChannel(ctx.channel()),
                2, TimeUnit.SECONDS);
    }
}
