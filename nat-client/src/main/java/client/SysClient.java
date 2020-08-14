package client;

import client.handler.SysClientHandler;
import client.reconnection.ConnectionListener;
import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，满足系统业务
 */

public class SysClient extends Client {

    private static Bootstrap client = new Bootstrap();
    private String host;
    private int port;

    public void init() {
        //加载配置文件
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
        host = cache.get("internal.host");
        port = cache.getInt("internal.port");
    }

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    //启动一条内部连接
    public void start() throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();
        client = new Bootstrap();
        client.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));
        ChannelFuture future;
        //进行连接
        try {
            synchronized (client) {
                client.handler(new ChannelInitializer<Channel>() {

                    //初始化channel
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                                        FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN),
                                new IdleStateHandler(FrameConstant.PIPELINE_READE_TIMEOUT_CONTROLL,
                                        FrameConstant.PIPELINE_WRITE_TIMEOUT,
                                        FrameConstant.PIPELINE_READ_WRITE_TIMEOUT),
                                //加入自定义的handler
                                new SysClientHandler());
                    }
                });
                future = client.connect(host, port);
                //启动监听，失败重连
                future.addListener(new ConnectionListener());
            }
            // 以下代码在synchronized同步块外面是安全的
            future.sync();
        } catch (Throwable t) {
            log.error("connects to {} fails", host+":"+port, t);
            throw new Exception("connects to Server fails", t);
        }
    }

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        SysClient sysClient = new SysClient();
        sysClient.init();
        sysClient.start();
    }
}
