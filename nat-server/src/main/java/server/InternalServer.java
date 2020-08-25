package server;

import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import server.decoder.ByteToPojoDecoder;
import server.decoder.PojoToByteEncoder;
import server.group.ServerChannelGroup;
import server.handler.InternalServerHandler;

import java.util.Objects;

/**
 * @Author wneck130@gmail.com
 * @function internal服务端，用来接受程序内部的internalClient的连接，从而打通服务端与客户端的网络隔绝
 */
public class InternalServer extends Server{
    public void init() {
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
        addShutdownHook();
    }

    public void start() throws Exception{
        bossGroup = new NioEventLoopGroup(FrameConstant.BOSSGROUP_NUM);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                        FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN))
//                        .addLast(new IdleStateHandler(0,0,1))
                        .addLast(new ByteToPojoDecoder())
                        .addLast(new PojoToByteEncoder())
//                        .addLast(new CustomEventHandler())
                        .addLast(new InternalServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt("internal.server.port")).sync();
        log.debug("InternalServer started on port " + cache.getInt("internal.server.port") + "......");
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

    public static void main(String[] args) throws Exception{
        InternalServer internalServer = new InternalServer();
        internalServer.init();
        ServerChannelGroup.printGroupState();
        internalServer.start();
    }
}
