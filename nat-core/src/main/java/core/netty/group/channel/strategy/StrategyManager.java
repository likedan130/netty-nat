package core.netty.group.channel.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public class StrategyManager {

    private static Map<Class<? extends ForkStrategy>, ForkStrategy> instances = new ConcurrentHashMap<>();

    /**
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static ForkStrategy getInstance(Class<? extends ForkStrategy> clazz) {
        Object instance = instances.get(clazz);
        if (instance == null) {
            synchronized (StrategyManager.class) {
                instance = instances.get(clazz);
                if (instance == null) {
                    try {
                        instance = clazz.newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    instances.put(clazz, (ForkStrategy) instance);
                }
            }
        }
        return (ForkStrategy) instance;
    }
}
