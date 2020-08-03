package server;

import core.cache.PropertiesCache;
import core.constant.NumberConstant;
import core.enums.StringEnum;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.handler.ProxyServerHandler;
import java.util.Objects;

public class ProxyServer extends Server {
    public void init() {
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
        addShutdownHook();
    }

    public void start() throws Exception{
        bossGroup = new NioEventLoopGroup(NumberConstant.ONE);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new ProxyServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, NumberConstant.ONE_THOUSAND_AND_TWENTY_FOUR)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("proxy.server.port")).sync();
        System.out.println("ProxyServer start listen on port " + cache.getInt("proxy.server.port") + "......");
        f.channel().closeFuture().sync();
    }

    @Override
    public boolean isStarted() {
        if (!Objects.equals(null, f) && f.channel().isActive()) {
            return true;
        }
        return false;
    }
}
