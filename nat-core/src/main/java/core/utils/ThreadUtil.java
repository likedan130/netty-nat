package core.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author wneck130@gmail.com
 */
public class ThreadUtil {
    
    /**
     * close the thread pool safely.
     * @param pool 
     */
    public static void safeClose(ExecutorService pool) {
        if(pool != null){
            pool.shutdown();
            try{
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            }catch(InterruptedException ex){
                //ignore the ex
            }
        }
    }

}
