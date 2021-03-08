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
import lombok.extern.slf4j.Slf4j;
import server.decoder.ByteToPojoDecoder;
import server.decoder.PojoToByteEncoder;
import server.group.ServerChannelGroup;
import server.handler.InternalServerHandler;

/**
 * @Author wneck130@gmail.com
 * @function internal服务端，用来接受程序内部的internalClient的连接，从而打通服务端与客户端的网络隔绝
 */
@Slf4j
public class InternalServer extends BaseServer {

    /**
     * 代理程序对外开发的端口
     */
    private static String PORT = "internal.server.port";

    public void init() throws Exception{
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
                        .addLast(new ByteToPojoDecoder())
                        .addLast(new PojoToByteEncoder())
                        .addLast(new InternalServerHandler());
            }
        };
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                .childHandler(channelInit)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        f = b.bind(cache.getInt(PORT)).sync();
        log.debug("InternalServer started on port {}......", cache.getInt(PORT));
        f.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws Exception{
        InternalServer internalServer = new InternalServer();
        internalServer.init();
        ServerChannelGroup.printGroupState();
        internalServer.start();
    }
}
