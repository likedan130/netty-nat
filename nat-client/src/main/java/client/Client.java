package client;

import core.cache.PropertiesCache;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.*;

public class Client {
    protected static final InternalLogger log = InternalLoggerFactory.getInstance(Client.class);
    protected EventLoopGroup group;
    protected ChannelFuture f;
    protected PropertiesCache cache;
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
    private static int keepAliveTime = 10;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    public static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    protected void doShutdown(){
        try{
            if(group != null){
                group.shutdownGracefully().sync();
            }

            if(threadPoolExecutor != null){
                threadPoolExecutor.shutdownNow();
            }
            log.debug("Server has been shutdown gracefully!");
        }catch(Exception ex){
            log.error("Error when shutdown server!!!");
        }
    }

    protected void addShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(){
            @Override
            public void run() {
                log.debug("执行 addShutdownHook...");
                doShutdown();
            }
        });
    }
}
