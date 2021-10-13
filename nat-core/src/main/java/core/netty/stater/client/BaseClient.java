package core.netty.stater.client;

import core.properties.cache.PropertiesCache;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author wneck130@gmail.com
 * @function 客户端基类
 */
@Slf4j
@Getter
@Setter
public class BaseClient {
    protected EventLoopGroup group;
    protected PropertiesCache cache;
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
    private static int keepAliveTime = 30;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    public static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public ChannelFuture future;

    protected String host;
    protected Integer port;
    protected void doShutdown(){
        try{
            if(group != null){
                group.shutdownGracefully().sync();
            }

            if(threadPoolExecutor != null){
                threadPoolExecutor.shutdownNow();
            }
            log.debug("BaseClient has been shutdown gracefully!");
        }catch(Exception ex){
            log.error("Error when shutdown client!!!");
        }
    }

    protected void addShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(() -> {
            log.debug("执行ShutdownHook...");
            doShutdown();
        }));
    }
}
