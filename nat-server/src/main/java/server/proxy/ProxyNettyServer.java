package server.proxy;

import core.entity.Frame;
import core.entity.Tunnel;
import core.netty.group.ServerChannelGroup;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import core.properties.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.netty.handler.DispatcherHandler;
import core.netty.stater.server.BaseServer;
import core.netty.stater.server.NettyServer;
import core.utils.ByteUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;
import server.proxy.handler.HttpProxyServerHandler;
import server.proxy.handler.TcpInLogHandler;
import server.proxy.handler.TcpOutLogHandler;
import server.proxy.handler.TcpProxyServerHandler;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/26
 */
@Slf4j
public class ProxyNettyServer extends BaseServer implements NettyServer {

    @Override
    public void init() {
        cache = PropertiesCache.getInstance();
        genericFutureListener = (ch) -> {
            nioServerFuture.channel().eventLoop()
                    .schedule(() -> this.start(port), 10, TimeUnit.SECONDS);
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
                    ch.pipeline()
                            //闲置连接回收
                            .addLast(new IdleStateHandler(READ_IDLE, WRITE_IDLE, ALL_IDLE, TimeUnit.MINUTES))
                            //inbound流日志记录
                            .addLast(new TcpInLogHandler())
                            //outbound流日志记录
                            .addLast(new TcpOutLogHandler())
                            //业务处理
//                            .addLast(new TcpProxyServerHandler());
                            .addLast("dispatcher", new DispatcherHandler() {
                                /**
                                 * 根据传输的数据内容识别出TCP/HTTP协议类型后进行pipeline动态调整
                                 * @param ctx
                                 */
                                @Override
                                public void addHttpHandler(ChannelHandlerContext ctx) {
                                    // server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
                                    ch.pipeline().addLast(new HttpServerCodec())
                                            .addLast(new HttpObjectAggregator(512 * 1024))
                                            .addLast(new HttpProxyServerHandler());
                                }

                                /**
                                 * 根据传输的数据内容识别出TCP/HTTP协议类型后进行pipeline动态调整
                                 * @param ctx
                                 */
                                @Override
                                public void addTcpHandler(ChannelHandlerContext ctx) {
                                    ctx.pipeline()
                                            //闲置连接回收
                                            .addLast(new IdleStateHandler(READ_IDLE, WRITE_IDLE, ALL_IDLE, TimeUnit.MINUTES))
                                            //inbound流日志记录
                                            .addLast(new TcpInLogHandler())
                                            //outbound流日志记录
                                            .addLast(new TcpOutLogHandler())
                                            //业务处理
                                            .addLast(new TcpProxyServerHandler());
                                }
                                /**
                                 * 外部请求与ProxyServer激活channel连接时，通知ProxyClient与被代理服务预建立连接
                                 */
                                @Override
                                public void fullyConnect(ChannelHandlerContext ctx) throws Exception {
                                    //发送启动代理客户端命令
                                    Frame frame = new Frame();
                                    frame.setReq(ctx.channel().id().asShortText());
                                    frame.setRes(FrameConstant.DEFAULT_CHANNEL_ID);
                                    frame.setCmd(ProcessorEnum.PRE_CONNECT.getCmd());
                                    //通过当前channel的parent channel获取对应的tunnelId
                                    Tunnel tunnel = ServerChannelGroup.getTunnelByChannel(ctx.channel().parent());
                                    Channel internalChannel = ServerChannelGroup.forkChannel(ForkStrategyEnum.MIN_LOAD);
                                    LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
                                    String[] clientHostSegment = tunnel.getClientHost().split("\\.");
                                    data.put("host", new byte[]{ByteUtil.fromInt(Integer.parseInt(clientHostSegment[0]))[3],
                                            ByteUtil.fromInt(Integer.parseInt(clientHostSegment[1]))[3],
                                            ByteUtil.fromInt(Integer.parseInt(clientHostSegment[2]))[3],
                                            ByteUtil.fromInt(Integer.parseInt(clientHostSegment[3]))[3]});
                                    data.put("port", new byte[]{ByteUtil.fromInt(tunnel.getClientPort())[2],
                                            ByteUtil.fromInt(tunnel.getClientPort())[3]});
                                    frame.setData(data);
                                    internalChannel.writeAndFlush(frame);
                                }
                            });

                }
            };
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, FrameConstant.TCP_SO_BACKLOG)
                    .option(ChannelOption.SO_REUSEADDR, FrameConstant.TCP_REUSE_ADDR)
                    .childHandler(channelInit)
                    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

            nioServerFuture = b.bind(port).sync();
            log.debug("ProxyServer started on port {}......", port);
            //添加关闭重启的监听器，3秒后尝试重启
            nioServerFuture.channel().closeFuture().addListener(genericFutureListener);
//            nioServerFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("ProxyServer started failed while listening on port {}！！！", port, e);
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
