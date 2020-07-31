package client;

import client.reconnection.AbstractConnectionWatchdog;
import client.reconnection.ConnectionListener;
import core.cache.PropertiesCache;
import core.frame.loader.PropertiesLoader;
import client.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，满足系统业务
 */
@Slf4j
public class SysClient extends Client {

    private static Bootstrap client = new Bootstrap();
    private String host;
    private int port;

    public void init() {
        //加载配置文件
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
        host = "127.0.0.1";
        port = 8080;
    }

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    //启动一条内部连接
    public void start() throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();

        client = new Bootstrap();
        client.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));

        final AbstractConnectionWatchdog watchdog = new AbstractConnectionWatchdog(client, timer, port, host, true) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new LengthFieldBasedFrameDecoder(65535, 10, 2),
                        new IdleStateHandler(0, 0, 10),
                        //加入自定义的handler
                        new SysClientHandler()
                };
            }
        };
        ChannelFuture future;
        //进行连接
        try {
            synchronized (client) {
                client.handler(new ChannelInitializer<Channel>() {

                    //初始化channel
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });
                future = client.connect(host, port);
                //启动监听，失败重连
                future.addListener(new ConnectionListener());
            }
            // 以下代码在synchronized同步块外面是安全的
            future.sync();
        } catch (Throwable t) {
            log.error("connects to  fails", t);
            throw new Exception("connects to  fails", t);
        }
    }

    public static void main(String[] args) throws Exception {
        SysClient sysClient = new SysClient();
        sysClient.init();
        sysClient.start();
    }
}
