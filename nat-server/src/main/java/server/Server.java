package server;

import core.cache.PropertiesCache;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.*;

/**
 * @Author wneck130@gmail.com
 * @Function 服务基类
 */
public abstract class Server {

    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ChannelFuture f;
    protected PropertiesCache cache;
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
    private static int keepAliveTime = 10;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    public static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public static InternalServer internalServer = new InternalServer();

    public static ProxyServer proxyServer = new ProxyServer();

    protected void doShutdown(){
        try{
            if(bossGroup != null){
                bossGroup.shutdownGracefully().sync();
            }
            if(workerGroup != null){
                workerGroup.shutdownGracefully().sync();
            }

            if(threadPoolExecutor != null){
                threadPoolExecutor.shutdownNow();
            }
            System.out.println("Server has been shutdown gracefully!");
        }catch(Exception ex){
            System.out.println("Error when shutdown server!!!");
        }
    }

    protected void addShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(){
            @Override
            public void run() {
                System.out.println("执行 addShutdownHook...");
                doShutdown();
            }
        });
    }

    abstract boolean isStarted();
}
