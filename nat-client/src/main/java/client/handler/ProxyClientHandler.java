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
import java.util.HashMap;
import java.util.Map;

/**
 * @Author wneck130@gmail.com
 * @function 代理客户端业务handler
 */
@Slf4j
public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] print = new byte[5];
        msg.getBytes(0, print);
        byte[] message = new byte[msg.readableBytes()];
        msg.readBytes(message);
        //获取内部客户端
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            Channel internalChannel = ClientChannelGroup.getInternalByProxy(ctx.channel().id());
            log.debug("Responsor数据："+ ByteUtil.toHexString(message)
                            + "\n Server--[{}]--ClientInternal----ClientProxy<<[{}]<<Responsor"
                    , internalChannel.id(), ctx.channel().id());
            //判断是否已经建立配对关系
            if (internalChannel != null) {
                //判断客户端channel是否活跃
                if (internalChannel != null && internalChannel.isActive()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("data", message);
                    Frame frame = new Frame();
                    frame.setCmd(CommandEnum.CMD_DATA_TRANSFER.getCmd());
                    frame.setData(map);
                    internalChannel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.debug("Responsor数据："+ ByteUtil.toHexString(message)
                                            + "\n Server<<[{}]<<ClientInternal----ClientProxy--[{}]--Responsor"
                                    , internalChannel.id(), ctx.channel().id());
                        }
                    });
                }
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.addProxyChannel(ctx.channel());
        ClientChannelGroup.printGroupState();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyChannel = ctx.channel();
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.debug("proxyChannel："+ proxyChannel.id() + " 断开连接!!!");
        if (ClientChannelGroup.channelPairExist(proxyChannel.id())) {
            ClientChannelGroup.printGroupState();
            Channel internalChannel = ClientChannelGroup.getInternalByProxy(proxyChannel.id());
            ClientChannelGroup.removeProxyChannel(proxyChannel);
            if (internalChannel ==  null) {
                log.error("与proxyChannel:"+proxyChannel.id()+"配对的internalChannel为null");
                return;
            }
            ClientChannelGroup.removeChannelPair(internalChannel.id(), proxyChannel.id());
            ClientChannelGroup.releaseInternalChannel(internalChannel);
        } else {
            log.error("proxyChannel："+ proxyChannel.id() + " 未找到配对关系!!!");
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
            log.debug("被代理客户端 -- "+ channel.remoteAddress()+ "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        }else{
            ctx.fireExceptionCaught(cause);
            log.debug("channel异常：",cause);
        }
    }
}
