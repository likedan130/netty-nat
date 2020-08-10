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
import server.handler.*;

import java.util.Objects;
public class InternalServer extends Server{
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
                ch.pipeline().addLast(new InternalServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, NumberConstant.ONE_THOUSAND_AND_TWENTY_FOUR)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("internal.server.port")).sync();
        log.debug("InternalServer start internal-server on port " + cache.getInt("internal.server.port") + "......");
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
