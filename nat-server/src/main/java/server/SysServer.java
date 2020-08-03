package server;

import core.cache.PropertiesCache;
import core.constant.NumberConstant;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import server.handler.SysServerhandler;

import java.io.File;
import java.util.Objects;

/**
 * @Author wneck130@gmail.com
 * @Function netty服务端，用来和代理程序的客户端建立TCP连接，满足系统业务
 */
public class SysServer extends Server {
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
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(NumberConstant.SIXTY_FIVE_THOUSAND_FIVE_HUNDRED_AND_THIRTY_FIVE,NumberConstant.TEN,NumberConstant.TWO))
                        .addLast(new IdleStateHandler(NumberConstant.ZERO, NumberConstant.ZERO, NumberConstant.TEN))
                        .addLast(new SysServerhandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, NumberConstant.ONE_THOUSAND_AND_TWENTY_FOUR)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("sys.server.port")).sync();
        log.info("SysServer start listen on port " + cache.getInt("sys.server.port") + "......");
        f.channel().closeFuture().sync();
    }

    @Override
    public boolean isStarted() {
        if (!Objects.equals(null, f) && f.channel().isActive()) {
            return true;
        }
        return false;
    }

    public static void main(String []args) throws Exception{
        SysServer sysServer = new SysServer();
        sysServer.init();
        sysServer.start();
    }
}
