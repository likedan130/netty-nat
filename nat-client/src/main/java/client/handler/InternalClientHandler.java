package client.handler;

import client.group.ClientChannelGroup;
import client.handler.processor.*;
import core.entity.Frame;
import core.enums.CommandEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @function 业务处理handler，所有协议命令在本类中处理
 */
@Slf4j
public class InternalClientHandler extends SimpleChannelInboundHandler<Frame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        log.debug("Server数据：" + msg.toString());
        byte cmd = msg.getCmd();
        switch (cmd) {
            //接入命令
            case 0x01:
                new LoginProcessor().process(ctx, msg);
                break;
            //心跳命令
            case 0x02:
                new HeartbeatProcessor().process(ctx, msg);
                break;
            //建立客户端代理连接
            case 0x03:
                new ConnectToProxyProcessor().process(ctx, msg);
                break;
            //连接池回收
            case 0x04:
                new ChannelRecycleProcessor().process(ctx, msg);
                break;
            case 0x05:
                new ConnectToProxyProcessor().process(ctx, msg);
                break;
            //消息转发
            case (byte)0xff:
                new DataTransferProcessor().process(ctx, msg);
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.addIdleInternalChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel internalChannel = ctx.channel();
        if (ClientChannelGroup.channelPairExist(internalChannel.id())) {
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(internalChannel.id());
            if (proxyChannel == null) {
                ClientChannelGroup.removeChannelPair(internalChannel.id());
            } else {
                ClientChannelGroup.removeChannelPair(internalChannel.id(), proxyChannel.id());
                //internalChannel断开连接，则内部通信已经中断，防止TCP包有拆包，所以直接断开proxyChannel
                ClientChannelGroup.removeProxyChannel(proxyChannel);
                ClientChannelGroup.removeInternalChannel(internalChannel);
                proxyChannel.flush().close();
            }
        }
        ClientChannelGroup.removeIdleInternalChannel(internalChannel);
    }

    /**
     * 通道异常触发
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ClientInternal[{}]发生异常：{}", ctx.channel().id(), cause.getStackTrace());
        ctx.close();
    }
}
