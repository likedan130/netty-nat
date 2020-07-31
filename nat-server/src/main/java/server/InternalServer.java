package server;

import core.cache.PropertiesCache;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import server.handler.*;

import java.util.Objects;
@Slf4j
public class InternalServer extends Server{

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
                ch.pipeline().addLast(new InternalServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(8082).sync();
        log.info("InternalServer start internal-server on port " + 8082 + "......");
        //服务端管道关闭的监听器并同步阻塞,直到server channel关闭,线程才会往下执行,结束进程
        f.channel().closeFuture().sync();
    }

    @Override
    public boolean isStarted() {
        //判端TCP连接是否活跃（channel()不为空 && 连接活跃）
        if (!Objects.equals(null, f) && f.channel().isActive()) {
            return true;
        }
        return false;
    }
}
