package client.proxy.handler;

import core.netty.group.ClientChannelGroup;
import client.internal.handler.processor.constant.ProcessorEnum;
import core.entity.Frame;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

/**
 * @Author wneck130@gmail.com
 * @function 代理客户端业务handler，在收到被代理服务的消息时将其转发给服务端
 */
@Slf4j
public class ProxyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String clientChannelId = ctx.channel().id().asShortText();
        String serverChannelId = ClientChannelGroup.getServerChannel(clientChannelId);
        //没有对应的服务端channel，直接返回
        if (serverChannelId.isEmpty()) {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                serverChannelId = ClientChannelGroup.getServerChannel(clientChannelId);
                if (!serverChannelId.isEmpty()) {
                    break;
                }
                Thread.sleep(200);
            }
            if (serverChannelId.isEmpty()) {
                ctx.close();
                return;
            }
        }
        Frame frame = new Frame();
        frame.setReq(clientChannelId);
        frame.setRes(serverChannelId);
        frame.setCmd(ProcessorEnum.UP_STREAM.getCmd());
        frame.setLen(msg.readableBytes());
        LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("data", BufUtil.getArray(msg));
        frame.setData(dataMap);
        Channel internalChannel = ClientChannelGroup.forkChannel(serverChannelId);
        internalChannel.writeAndFlush(frame);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelGroup.addProxy(ctx.channel());
        ClientChannelGroup.printGroupState();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //清除缓存信息
        String clientChannelId = ctx.channel().id().asShortText();
        String serverChannelId = ClientChannelGroup.getServerChannel(clientChannelId);
        ClientChannelGroup.removeChannelPair(serverChannelId, clientChannelId);
        ClientChannelGroup.removeProxy(ctx.channel());
    }

    /**
     * 通道异常触发
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if (!channel.isActive()) {
            log.debug("被代理客户端 -- " + channel.remoteAddress() + "  断开了连接！");
            cause.printStackTrace();
            ctx.close();
        } else {
            ctx.fireExceptionCaught(cause);
            log.error("channel异常：", cause);
        }
    }
}
