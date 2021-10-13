package server.proxy.handler;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.entity.Tunnel;
import core.netty.group.ServerChannelGroup;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;

/**
 * @Author wneck130@gmail.com
 * @Function proxyServer业务handler
 */
@Slf4j
public class TcpProxyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 数据传输时触发
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //收到外部的msg消息，首先挑选一条internalChannel准备进行数据转发
        String serverChannelId = ctx.channel().id().asShortText();
        String clientChannelId = ServerChannelGroup.getClientByServer(serverChannelId);
        //没有对应的服务端channel，直接返回
        if (clientChannelId.isEmpty()) {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                clientChannelId = ServerChannelGroup.getClientByServer(serverChannelId);
                if (!clientChannelId.isEmpty()) {
                    break;
                }
                Thread.sleep(200);
            }
            if (clientChannelId.isEmpty()) {
                ctx.close();
                return;
            }
        }
        Channel internalChannel = ServerChannelGroup.forkChannel(ForkStrategyEnum.MIN_LOAD);
        //第二步，封装成内部通信的指定格式
        byte[] message = new byte[msg.readableBytes()];
        msg.readBytes(message);
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(1);
        map.put("data", message);
        Frame frame = new Frame();
        frame.setCmd(ProcessorEnum.DOWN_STREAM.getCmd());
        frame.setReq(serverChannelId);
        frame.setRes(clientChannelId);
        frame.setData(map);
        internalChannel.writeAndFlush(frame);
    }

    /**
     * 通道连接成功触发
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
        log.debug("{}:{}收到新的连接请求", inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        ServerChannelGroup.addProxy(ctx.channel());
        //新的连接建立后进行配对
        fullyConnect(ctx);
        ServerChannelGroup.printGroupState();
    }

    /**
     * 外部请求与ProxyServer激活channel连接时，通知ProxyClient与被代理服务预建立连接
     */
    public void fullyConnect(ChannelHandlerContext ctx) throws Exception {
        //发送启动代理客户端命令
        Frame frame = new Frame();
        frame.setReq(ctx.channel().id().asShortText());
        frame.setRes(FrameConstant.DEFAULT_CHANNEL_ID);
        frame.setCmd(ProcessorEnum.PRE_CONNECT.getCmd());
        //通过当前channel的parent channel获取对应的tunnelId
        Tunnel tunnel = ServerChannelGroup.getTunnelByChannel(ctx.channel().parent());
        Channel internalChannel = ServerChannelGroup.forkChannel(ForkStrategyEnum.MIN_LOAD);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
        String[] clientHostSegment = tunnel.getClientHost().split("\\.");
        data.put("host", new byte[]{ByteUtil.fromInt(Integer.parseInt(clientHostSegment[0]))[3],
                ByteUtil.fromInt(Integer.parseInt(clientHostSegment[1]))[3],
                ByteUtil.fromInt(Integer.parseInt(clientHostSegment[2]))[3],
                ByteUtil.fromInt(Integer.parseInt(clientHostSegment[3]))[3]});
        data.put("port", new byte[]{ByteUtil.fromInt(tunnel.getClientPort())[2],
                ByteUtil.fromInt(tunnel.getClientPort())[3]});
        frame.setData(data);
        internalChannel.writeAndFlush(frame);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerChannelGroup.removeProxy(ctx.channel());
    }
}
