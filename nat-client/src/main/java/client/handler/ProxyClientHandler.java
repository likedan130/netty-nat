package client.handler;

import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.enums.CommandEnum;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        byte[] print = new byte[5];
        msg.getBytes(0, print);
        log.debug("proxyClient:"+ctx.channel().id()+"收到数据："+ ByteUtil.toHexString(print));
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"  proxyClient收到数据："+ ByteUtil.toHexString(print));
        //判断是否已经建立配对关系
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            //获取内部客户端
            Channel internalChannel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
            //判断客户端channel是否活跃
            if (internalChannel != null && internalChannel.isActive()) {
                byte[] message = new byte[msg.readableBytes()];
                msg.readBytes(message);
                Map<String, Object> map = new HashMap<>();
                map.put("data", message);
                Frame frame = new Frame();
                frame.setCmd(CommandEnum.CMD_DATA_TRANSFER.getCmd());
                frame.setData(map);
                internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
//                        System.out.println("向"+internalChannel.id()+"回复业务数据成功!!!");
                        log.error("向{}回复业务数据{}成功!!!", internalChannel.id(), Arrays.copyOf(message, 5));
                    }
                });
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("proxyChannel:" + ctx.channel().id()+" 建立与被代理服务间的连接!!!");
//        System.out.println("proxyChannel:" + ctx.channel().id()+" 建立与被代理服务间的连接");
        ClientChannelGroup.addProxyChannel(ctx.channel());
        ClientChannelGroup.printGroupState();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyChannel = ctx.channel();
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.debug("proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
        if (ClientChannelGroup.channelPairExist(proxyChannel.id())) {
            ClientChannelGroup.printGroupState();
            Channel internalChannel = ClientChannelGroup.getInternalByProxy(proxyChannel.id());
            ClientChannelGroup.removeProxyChannel(proxyChannel);
            if (internalChannel ==  null) {
                log.error("与proxyChannel:"+proxyChannel.id()+"配对的internalChannel为null");
//                System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - internalChannel为null");
                return;
            }
            ClientChannelGroup.removeChannelPair(internalChannel.id(), proxyChannel.id());
            ClientChannelGroup.releaseInternalChannel(internalChannel);
        } else {
            log.error("proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
            ClientChannelGroup.printGroupState();
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
            log.debug("############### -- 被代理客户端 -- "+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
            ctx.fireExceptionCaught(cause);
            log.debug("###############",cause);
        }
    }
}
