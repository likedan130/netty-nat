package server.handler;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import server.Server;
import server.group.ServerChannelGroup;
import java.util.concurrent.TimeUnit;
@Slf4j
public class ProxyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        log.info("代理服务channel:"+ctx.channel().id()+"收到：" + ByteUtil.toHexString(BufUtil.getArray(msg)));
        //收到外部请求先找配对的内容连接
        Channel proxyChannel = ctx.channel();
        if (ServerChannelGroup.channelPairExist(proxyChannel.id())) {
            //已经存在配对，直接进行消息转发
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(proxyChannel.id());
            byte[] message = new byte[msg.readableBytes()];
            msg.readBytes(message);
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(message);
            internalChannel.writeAndFlush(byteBuf);
        } else {
            //未配对的，先进行配对后，再消息转发
            ServerChannelGroup.forkChannel(ctx.channel());
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(proxyChannel.id());
            byte[] message = new byte[msg.readableBytes()];
            msg.readBytes(message);
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(message);
            internalChannel.writeAndFlush(byteBuf);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //新的连接建立后先进行配对
        ServerChannelGroup.forkChannel(ctx.channel());
        //发送启动代理客户端
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(FrameConstant.pv);
        long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_PROXY_START.getCmd());
        byteBuf.writeShort(1);
        //计算校验和
        int vc = 0;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        byteBuf.writeByte(vc);
        Channel channel = ServerChannelGroup.getSysChannel().get("Sys");
        channel.writeAndFlush(byteBuf);
        Thread.sleep(2000);
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
