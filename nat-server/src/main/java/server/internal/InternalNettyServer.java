package server.internal;

import core.netty.group.channel.message.MessageContext;
import core.netty.handler.processor.ProcessorManager;
import core.properties.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.properties.loader.PropertiesLoader;
import core.netty.stater.server.BaseServer;
import core.netty.stater.server.NettyServer;
import core.properties.loader.YamlLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import server.internal.decoder.ByteToPojoDecoder;
import server.internal.decoder.PojoToByteEncoder;
import server.internal.handler.CustomEventHandler;
import server.internal.handler.InternalServerHandler;
import core.netty.handler.MessageSendFilter;
import core.netty.handler.MessageReceiveFilter;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/29
 */
@Slf4j
public class InternalNettyServer extends BaseServer implements NettyServer {

    /**
     * 心跳超时时间，默认为2倍心跳发送间隔，心跳超时后服务主动回收连接
     */
    private static long HEARTBEAT_TIMEOUT = 2 * 1L;

    /**
     * 代理程序对外开发的端口
     */
    private static String PORT = "internal.server.port";

    public static void main(String[] args) throws Exception {
        InternalNettyServer internalNettyServer = new InternalNettyServer();
        new YamlLoader().load(internalNettyServer.getClass().getResource("/").getPath());
        int port = PropertiesCache.getInstance().getInt(PORT);
        internalNettyServer.start(port);
    }

    @Override
    public void init() {
        cache = PropertiesCache.getInstance();
        genericFutureListener = (future) -> {
            //启动成功则为每个命令注册响应监听器
            if (future.isSuccess()) {
                MessageContext messageContext = MessageContext.getInstance();
                //将所有指令的处理器都指派给messageContext作为监听器
                Arrays.stream(ProcessorEnum.values()).forEach(processorEnum -> {
                    messageContext.addResponseListener(ProcessorManager.getInstance(processorEnum.getClazz()));
                });
            } else {
                //启动不成功在一定的延迟之后进行重启
                nioServerFuture.channel().eventLoop()
                        .schedule(() -> this.start(port), 10, TimeUnit.SECONDS);
            }
        };
        addShutdownHook();
    }

    @Override
    public void start(int port) {
        init();
        this.port = port;
        //如果已经启动了proxyServer，静默
        if (nioServerFuture != null && nioServerFuture.channel().isOpen()) {
            return;
        }
        try {
            bossGroup = new NioEventLoopGroup(FrameConstant.BOSSGROUP_NUM);
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            ChannelInitializer<SocketChannel> channelInit = new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                            FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN))
                            .addLast(new IdleStateHandler(0, 0, HEARTBEAT_TIMEOUT, TimeUnit.MINUTES))
                            .addLast(new ByteToPojoDecoder())
                            .addLast(new PojoToByteEncoder())
                            .addLast(new MessageReceiveFilter())
                            .addLast(new MessageSendFilter())
                            .addLast(new CustomEventHandler())
                            .addLast(new InternalServerHandler());
                }
            };
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                    .option(ChannelOption.SO_REUSEADDR, FrameConstant.TCP_REUSE_ADDR)
                    .childOption(ChannelOption.TCP_NODELAY, FrameConstant.TCP_NODELAY)
                    .childHandler(channelInit)
                    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

            nioServerFuture = b.bind(cache.getInt(PORT)).sync();
            log.debug("InternalServer started on port {}......", cache.getInt(PORT));
            //添加关闭重启的监听器，3秒后尝试重启
            nioServerFuture.addListener(genericFutureListener);
            nioServerFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("InternalServer started failed while listening on port {}！！！", cache.getInt(PORT), e);
        }
    }

    @Override
    public boolean isHeathy() {
        return nioServerFuture.channel().isOpen();
    }

    @Override
    public boolean isRunning() {
        return nioServerFuture.channel().isActive();
    }

    @Override
    public void close() {
        nioServerFuture.removeListener(genericFutureListener);
        nioServerFuture.channel().close();
        log.info("ProxyServer closed success");
    }
}
