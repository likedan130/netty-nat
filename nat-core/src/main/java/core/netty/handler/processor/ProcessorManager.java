package core.netty.handler.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/24
 */
public class ProcessorManager {

    private static Map<Class, Processor> instances = new ConcurrentHashMap<>();

    /**
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static Processor getInstance(Class clazz) {
        Object instance = instances.get(clazz);
        if (instance == null) {
            synchronized (ProcessorManager.class) {
                instance = instances.get(clazz);
                if (instance == null) {
                    try {
                        instance = clazz.newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    instances.put(clazz, (Processor) instance);
                }
            }
        }
        return (Processor) instance;
    }
}
