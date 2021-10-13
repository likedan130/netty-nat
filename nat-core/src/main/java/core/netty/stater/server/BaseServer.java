package core.netty.stater.server;

import core.properties.cache.PropertiesCache;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @Author wneck130@gmail.com
 * @Function 服务端基类
 */
@Slf4j
@Getter
public abstract class BaseServer {
    /**
     * 全局异步任务线程池
     */
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
    private static int keepAliveTime = 10;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    /**
     * boss线程组
     */
    protected EventLoopGroup bossGroup;
    /**
     * worker线程组
     */
    protected EventLoopGroup workerGroup;
    /**
     * NioServerSocketChannel对应的future
     */
    protected ChannelFuture nioServerFuture;

    /**
     * 通道读写超时配置项
     */
    protected final int READ_IDLE = 0;
    protected final int WRITE_IDLE = 0;
    protected final int ALL_IDLE = 10;

    /**
     * 代理程序对外提供的服务端口
     */
    protected static String PORT = "proxy.server.port";

    protected int port;

    protected GenericFutureListener genericFutureListener;

    protected PropertiesCache cache;

    protected void doShutdown() {
        try {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }

            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdownNow();
            }
            log.debug("BaseServer has been shutdown gracefully!");
        } catch (Exception ex) {
            log.debug("Error when shutdown server!!!");
        }
    }

    protected void addShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("执行 addShutdownHook...");
                doShutdown();
                close();
            }
        });
    }

    public abstract void close();
}
