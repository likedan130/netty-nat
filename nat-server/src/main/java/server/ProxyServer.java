package server;

import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import server.group.ServerChannelGroup;
import server.handler.HttpHandler;
import server.handler.ProxyServerHandler;

import java.util.Objects;

/**
 * @Author wneck130@gmail.com
 * @Function proxy服务端，用来接受来自外部的TCP请求
 */
public class ProxyServer extends Server {

    private ProxyServer () {
    }

    public static class Holder {
        private static ProxyServer instance = new ProxyServer();
    }

    public static ProxyServer getInstance() {
        return Holder.instance;
    }

    public void init() {
        cache = PropertiesCache.getInstance();
        addShutdownHook();
    }

    public synchronized void start() throws Exception{
        if (ServerChannelGroup.getProxyServerStarted()) {
            return;
        }
        bossGroup = new NioEventLoopGroup(FrameConstant.BOSSGROUP_NUM);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new HttpHandler())
                        .addLast(new ProxyServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("proxy.server.port")).sync().addListener((future -> {
            if (future.isSuccess()) {
                ServerChannelGroup.setProxyServerStarted(Boolean.TRUE);
            }
        }));
        log.debug("ProxyServer started on port " + cache.getInt("proxy.server.port") + "......");
        f.channel().closeFuture().sync();
        ServerChannelGroup.setProxyServerStarted(Boolean.FALSE);
        ProxyServer.getInstance().start();
    }

    @Override
    public boolean isStarted() {
        if (!Objects.equals(null, f) && f.channel().isActive()) {
            return true;
        }
        return false;
    }
}
