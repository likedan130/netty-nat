package server;

import core.cache.PropertiesCache;
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
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535,10,2))
                        .addLast(new IdleStateHandler(0, 0, 10))
                        .addLast(new SysServerhandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("sys.server.port")).sync();
        System.out.println("SysServer start listen on port " + cache.get("sys.server.port") + "......");
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
