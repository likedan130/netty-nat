package server;

import core.cache.PropertiesCache;
import core.constant.NumberConstant;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @Author wneck130@gmail.com
 * @Function 服务基类
 */
public abstract class Server {
    protected static final Logger log =LoggerFactory.getLogger(Server.class);
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ChannelFuture f;
    protected PropertiesCache cache;
    private static int corePoolSize = Runtime.getRuntime().availableProcessors() * NumberConstant.TWO + NumberConstant.ONE;
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors() * NumberConstant.THREE;
    private static int keepAliveTime = NumberConstant.TEN;
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
            maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(NumberConstant.ONE_HUNDRED),
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
            log.debug("Server has been shutdown gracefully!");
        }catch(Exception ex){
            log.debug("Error when shutdown server!!!");
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

    abstract boolean isStarted();
}
