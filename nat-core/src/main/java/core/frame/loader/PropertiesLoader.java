/** 
 * Project Name: farm-core 
 * File Name: PropertiesLoader.java 
 * Package Name: core.frame.loader
 * Date: 2017年3月20日上午10:47:55 
 * Copyright (c) 2017, hadlinks All Rights Reserved. 
 * 
 */
package core.frame.loader;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.logging.log4j.*;

import core.cache.PropertiesCache;
import core.frame.filewatch.FileWatchService;

/** 
 * ClassName: PropertiesLoader 
 * Function: 加载.properties的配置文件加载器，.  
 * date: 2017年3月20日 上午10:47:55  
 * 
 * @author songwei (songw@hadlinks.com)
 * @version 
 * @since JDK 1.8 
 */
public class PropertiesLoader extends AbstractLoader{

	private final Logger logger = LogManager.getLogger(PropertiesLoader.class);
	
	@Override
	public void load(String path) {
		try{
			Properties properties = new Properties();
	        FileInputStream in = new FileInputStream(new File(path+File.separator+"properties.properties"));
	        properties.load(in);
	        for(Object key : properties.keySet()){
	        	PropertiesCache.getInstance().getProps().putIfAbsent(key.toString(), properties.get(key).toString());
	        }
//	        //加载完成后，启动一个FileWatchService来对文件修改状态进行监控，如果监听到文件修改，则重新加载修改后的文件内容
//			FileWatchService service = new FileWatchService();
//			new Thread(() -> service.addWatcher(path, this)).start();
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e);
		}
	}

	@Override
	public void reload(String path) {
		load(path);		
	}

}
