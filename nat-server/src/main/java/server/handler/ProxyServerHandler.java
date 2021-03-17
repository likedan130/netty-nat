package server.handler;

import core.entity.Frame;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import server.group.ServerChannelGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author wneck130@gmail.com
 * @Function proxyServer业务handler
 */
@Slf4j
public class ProxyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 数据传输时触发
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        if (ServerChannelGroup.channelPairExist(ctx.channel().id())) {
            //已经存在配对，直接进行消息转发
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(ctx.channel().id());
            byte[] print = new byte[5];
            msg.getBytes(0, print);
            log.debug("\nRequestor数据："+ ByteUtil.toHexString(BufUtil.getArray(msg))
                +"\r\nRequestor>>[{}]>>ServerProxy----ServerInternal--[{}]--Client",
                    ctx.channel().id(), internalChannel.id());
            if(internalChannel != null) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                Map<String, Object> map = new HashMap<>();
                map.put("data", message);
                Frame frame = new Frame();
                frame.setCmd(CommandEnum.CMD_DATA_TRANSFER.getCmd());
                frame.setData(map);
                internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.error("发送数据异常: ", future.cause()
                                + "\nRequestor--[{}]--ServerProxy--XX--ServerInternal--[{}]--Client",
                                ctx.channel().id(), internalChannel.id());
                        //断开代理服务连接，重新建立channel配对并对外提供服务
                        ctx.channel().close();
                    }
                });
            }else {
                log.error("配对的internalChannel已失效!!!"
                    + "\r\nRequestor--[{}]--ServerProxy----ServerInternal--[XX]--Client",
                        ctx.channel().id());
                //断开代理服务连接，重新建立channel配对并对外提供服务
                ctx.channel().close();
            }
        } else {
            log.error("找不到配对的internalChannel!!!"
                            + "\r\nRequestor--[{}]--ServerProxy----ServerInternal--[XX]--Client",
                    ctx.channel().id());
            //断开代理服务连接，重新建立channel配对并对外提供服务
            ctx.channel().close();
        }
    }

    /**
     * 通道连接成功触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //新的连接建立后进行配对
        Channel internalChannel = ServerChannelGroup.forkChannel(ctx.channel());
        fullyConnect(internalChannel);
        ServerChannelGroup.printGroupState();
        log.debug("建立连接：Requestor--[{}]--ServerProxy----ServerInternal--[{}]--Client",
                ctx.channel().id(), internalChannel.id());
    }

    /**
     * 通道断开时触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyChannel = ctx.channel();
        log.debug("proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
        if (ServerChannelGroup.channelPairExist(proxyChannel.id())) {
            ServerChannelGroup.printGroupState();
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(proxyChannel.id());
            ServerChannelGroup.removeProxyChannel(ctx.channel());
            if (internalChannel ==  null) {
                log.error("与proxyChannel："+proxyChannel.id()+"配对的internalChannel为null");
                return;
            }
            ServerChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
            ServerChannelGroup.releaseInternalChannel(internalChannel);
            //确保客户端连接情况和服务端一致，发送连接回收命令给客户端
            Frame frame = new Frame();
            frame.setCmd(CommandEnum.CMD_CHANNEL_RECYCLE.getCmd());
            internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("发送连接回收命令异常: ", future.cause()
                                    + "\nRequestor--[{}]--ServerProxy--XX--ServerInternal--[{}]--Client",
                            ctx.channel().id(), internalChannel.id());
                }
            });
        } else {
            log.error("proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
            ServerChannelGroup.printGroupState();
        }
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
            ctx.close();
        }
        log.error("ServerProxy[{}]发生异常：{}", ctx.channel().id(), cause.getStackTrace());
    }

    /**
     * proxyChannel与internalChannel建立配对后，通知客户端建立与被代理服务的连接并缓存配对关系
     */
    public void fullyConnect(Channel internalChannel) throws Exception{
        //发送启动代理客户端命令
        Frame frame = new Frame();
        frame.setCmd(CommandEnum.CMD_START_PROXY_CLIENT.getCmd());
        Channel proxyChannel = ServerChannelGroup.getProxyByInternal(internalChannel.id());
        internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.debug("Server数据发送异常："+ frame.toString()
                                +"\r\nRequestor--[{}]--ServerProxy--XX--ServerInternal--[{}]--Client",
                        proxyChannel.id(), internalChannel.id());
            } else {
                log.debug("Server数据："+ frame.toString()
                                +"\r\nRequestor--[{}]--ServerProxy>>>>ServerInternal--[{}]--Client",
                        proxyChannel.id(), internalChannel.id());
            }
        });
    }
}
