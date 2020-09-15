/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.cache;

import java.util.concurrent.*;
import core.utils.ByteUtil;

/**
 * Project Name: huamei-farm 
 * Package Name: core.cache
 * ClassName: PropertiesCache 
 * Function: 缓存项目的全局配置信息.  
 * date: 2017/3/15 10:53
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public class PropertiesCache {
	
    /**
     * 私有的空参构造函数
     */
    private PropertiesCache(){}

    /**
     * 单例
     * @return
     */
    public static PropertiesCache getInstance(){
        return Holder.INSTANCE;
    }

    /**
     * 懒加载
     * @author songw
     *
     */
    private static class Holder{
        final static PropertiesCache INSTANCE = new PropertiesCache();
    }

    private final ConcurrentMap<String, String> props = new ConcurrentHashMap<String, String>();

    
    public ConcurrentMap<String, String> getProps() {
		return props;
	}

	public String get(String key){
        return props.get(key);
    }
	
	public Integer getInt(String key){
		return Integer.valueOf(props.get(key));
	}
	
	public Byte getByte(String key){
		return ByteUtil.parseHexString(props.get(key));
	}
	
	public byte[] getBytes(String key){
		return ByteUtil.parseHexStringToArray(props.get(key));
	}
}
