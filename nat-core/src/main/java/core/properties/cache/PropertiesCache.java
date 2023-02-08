package core.properties.cache;

import java.util.List;
import java.util.concurrent.*;

import core.utils.ByteUtil;

/**
 * Project Name: nat-core
 * Package Name: core.properties.cache
 * ClassName: PropertiesCache
 * Function: 缓存项目的全局配置信息.
 * date: 2017/3/15 10:53
 *
 * @author wneck130@gmail.com
 * @since JDK 1.8
 */
public class PropertiesCache {

    /**
     * 私有的空参构造函数
     */
    private PropertiesCache() {
    }

    /**
     * 单例
     *
     * @return
     */
    public static PropertiesCache getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 懒加载
     *
     * @author songw
     */
    private static class Holder {
        final static PropertiesCache INSTANCE = new PropertiesCache();
    }

    private final ConcurrentMap<String, Object> props = new ConcurrentHashMap<>();


    public ConcurrentMap<String, Object> getProps() {
        return props;
    }

    public String get(String key) {
        return props.get(key).toString();
    }

    public Integer getInt(String key) {
        return Integer.valueOf(this.get(key));
    }

    public Byte getByte(String key) {
        return ByteUtil.parseHexString(this.get(key));
    }

    public byte[] getBytes(String key) {
        return ByteUtil.parseHexStringToArray(this.get(key));
    }

    public List<Object> getList(String key) {
        return (List<Object>) props.get(key);
    }
}
