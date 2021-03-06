package server;

import core.cache.PropertiesCache;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @Author wneck130@gmail.com
 * @Function 服务端基类
 */
@Slf4j
public abstract class BaseServer {
    public static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
    private static int keepAliveTime = 10;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ChannelFuture f;
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
                log.debug("执行 addShutdownHook...");
                doShutdown();
            }
        });
    }
}
