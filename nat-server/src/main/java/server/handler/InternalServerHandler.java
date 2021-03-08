package server.handler;

import core.entity.Frame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ProxyServer;
import server.group.ServerChannelGroup;
import server.handler.processor.*;

/**
 * @Author wneck130@gmail.com
 * @function 业务处理handler，所有协议命令在本类中处理
 */
@Slf4j
public class InternalServerHandler extends SimpleChannelInboundHandler<Frame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
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
            //连接池扩容
            case 0x03:
                new ConnectionExpandProcessor().process(ctx, msg);
                break;
            //连接池回收
            case 0x04:
                new ConnectionReduceProcessor().process(ctx, msg);
                break;
            //消息转发
            case (byte)0xff:
                new DataTransferProcessor().process(ctx, msg);
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //当第一条可用的internalChannel建立时，启动proxyServer开始提供代理服务
        if (ServerChannelGroup.idleInternalGroupIsEmpty()) {
            startProxyServer(ctx);
        } else {
            ServerChannelGroup.addIdleInternalChannel(ctx.channel());
        }
        log.debug("建立连接：ServerInternal--[{}]--ClientInternal", ctx.channel().id());
    }

    /**
     * 启动代理服务端暂时使用锁保证一次
     * @param ctx
     */
    static synchronized void startProxyServer(ChannelHandlerContext ctx) {
        if (ServerChannelGroup.idleInternalGroupIsEmpty()) {
            new Thread(() -> {
                try {
                    ProxyServer proxyServer = new ProxyServer();
                    proxyServer.init();
                    proxyServer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("启动ProxyServer异常:" + e);
                }
            }).start();
            ServerChannelGroup.addIdleInternalChannel(ctx.channel());
        } else {
            ServerChannelGroup.addIdleInternalChannel(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //普通internalChannel断开连接，则回收idleInternalGroup和internalGroup中的连接，等待客户端重新发起连接补足连接数
        ServerChannelGroup.removeIdleInternalChannel(ctx.channel());
        ServerChannelGroup.removeInternalChannel(ctx.channel());
    }

    /**
     * 通道异常触发
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ServerInternal[{}]发生异常：{}", ctx.channel().id(), cause.getStackTrace());
        ctx.close();
    }
}
