package server.handler;

import core.entity.Frame;
import core.enums.CommandEnum;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import server.group.ServerChannelGroup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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
        //收到外部请求先找配对的内容连接
        Channel proxyChannel = ctx.channel();
        if (ServerChannelGroup.channelPairExist(proxyChannel.id())) {
            //已经存在配对，直接进行消息转发
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(proxyChannel.id());
//            logger.debug("外部服务请求发送数据-"+proxyChannel.id());
            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            byte[] print = new byte[5];
            msg.getBytes(0, print);
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+" proxyChannel收到数据："+ ByteUtil.toHexString(print));
            log.debug("proxyChannel收到数据："+ ByteUtil.toHexString(print));
            if(internalChannel != null && internalChannel.isActive()) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                Map<String, Object> map = new HashMap<>();
                map.put("data", message);
                Frame frame = new Frame();
                frame.setCmd(CommandEnum.CMD_DATA_TRANSFER.getCmd());
                frame.setData(map);
                internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.error("send data to proxyServer exception occur: ", future.cause());
                    }
                });
            }else {
                log.error("ProxyServerHandler channel is closed");
            }
        } else {
            log.error("ProxyServerHandler No matching association");
        }
    }

    /**
     * 通道连接成功触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("外部服务请求建立连接："+ctx.channel().id());
        //新的连接建立后进行配对
        Channel internalChannel = ServerChannelGroup.forkChannel(ctx.channel());
        fullyConnect(internalChannel);
        ServerChannelGroup.printGroupState();
    }

    /**
     * 通道断开时触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyChannel = ctx.channel();
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.debug("proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
        if (ServerChannelGroup.channelPairExist(proxyChannel.id())) {
            ServerChannelGroup.printGroupState();
            Channel internalChannel = ServerChannelGroup.getInternalByProxy(proxyChannel.id());
            ServerChannelGroup.removeProxyChannel(ctx.channel());
            if (internalChannel ==  null) {
                log.error("与proxyChannel："+proxyChannel.id()+"配对的internalChannel为null");
//                System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"internalChannel为null");
                return;
            }
            ServerChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
            ServerChannelGroup.releaseInternalChannel(internalChannel);
            //确保客户端连接情况和服务端一致，发送连接回收命令给客户端
            Frame frame = new Frame();
            frame.setCmd(CommandEnum.CMD_CHANNEL_RECYCLE.getCmd());
            internalChannel.writeAndFlush(frame);
        } else {
            log.error("proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
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
            log.debug("############### -- 代理服务 -- "+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
//            ctx.fireExceptionCaught(cause);
            cause.printStackTrace();
//            logger.debug("###############",cause);
        }
    }

    /**
     * proxyChannel与internalChannel建立配对后，通知客户端建立与被代理服务的连接并缓存配对关系
     */
    public void fullyConnect(Channel internalChannel) {
        //发送启动代理客户端命令
        Frame frame = new Frame();
        frame.setCmd(CommandEnum.CMD_START_PROXY_CLIENT.getCmd());
        internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("proxyServer send data to sysClient exception occur: ", future.cause());
            } else {
//                System.out.println(internalChannel.id() + "发送建立代理连接指令!!!");
                log.debug(internalChannel.id() + "发送建立代理连接指令!!!");
            }
        });
    }
}
