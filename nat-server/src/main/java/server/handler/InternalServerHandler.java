package server.handler;

import core.entity.Frame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ProxyServer;
import server.group.ServerChannelGroup;
import server.handler.processor.*;

/**
 * @Author wneck130@gmail.com
 * @function 业务处理handler，所有协议命令在本类中处理
 */
public class InternalServerHandler extends SimpleChannelInboundHandler<Frame> {
    private static final Logger logger = LoggerFactory.getLogger(InternalServerHandler.class);
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
            new Thread(() -> {
                try {
                    ProxyServer proxyServer = ProxyServer.getInstance();
                    proxyServer.init();
                    proxyServer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("启动ProxyServer异常:" + e);
                }
            }).start();
//            new Thread(() -> {
//                try {
//                    ProxyServer proxyServer = ProxyServer.getInstance();
//                    proxyServer.init();
//                    proxyServer.startHttp();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    logger.error("启动ProxyServer异常:" + e);
//                }
//            }).start();
        }
        ServerChannelGroup.addIdleInternalChannel(ctx.channel());
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
