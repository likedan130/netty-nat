package server;

import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import server.group.ServerChannelGroup;
import server.handler.ConverterHandler;
import server.handler.HttpHandler;
import server.handler.ProxyServerHandler;

/**
 * @Author wneck130@gmail.com
 * @Function proxy服务端，用来接受来自外部的TCP请求
 */
@Slf4j
public class ProxyServer extends BaseServer {

    /**
     * 代理程序对外提供的服务端口
     */
    private static String PORT = "proxy.server.port";

    public void init() {
        cache = PropertiesCache.getInstance();
        addShutdownHook();
    }

    public synchronized void start() throws Exception{
        //如果已经启动了proxyServer，静默
        if (ServerChannelGroup.proxyServer != null) {
            return;
        }
        bossGroup = new NioEventLoopGroup(FrameConstant.BOSSGROUP_NUM);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("converter", new ConverterHandler())
                        .addLast("http", new HttpHandler())
                        .addLast(new ProxyServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt(PORT)).sync().addListener((future -> {
            if (future.isSuccess()) {
                ServerChannelGroup.proxyServer = f;
            }
        }));
        log.debug("ProxyServer started on port " + cache.getInt(PORT) + "......");
        f.channel().closeFuture().sync();
        ServerChannelGroup.proxyServer = null;
    }
}
