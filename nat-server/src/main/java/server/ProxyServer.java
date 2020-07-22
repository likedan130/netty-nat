package server;

import core.cache.PropertiesCache;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import server.handler.ProxyServerHandler;
import server.handler.ProxyServerHandler2;
import server.handler.SysServerhandler;

import java.util.Objects;

public class ProxyServer extends Server {

    public void init() {
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
        addShutdownHook();
    }

    public void start() throws Exception{
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new ProxyServerHandler2());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(8081).sync();
        System.out.println("SysServer start listen on port " + 8081 + "......");
        f.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws Exception {
        ProxyServer server = new ProxyServer();
        server.init();
        server.start();
    }
    @Override
    public boolean isStarted() {
        if (!Objects.equals(null, f) && f.channel().isActive()) {
            return true;
        }
        return false;
    }
}
