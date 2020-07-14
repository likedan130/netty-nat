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
import java.util.ArrayList;
import java.util.List;
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
            //读取文件
            List<File> fileList = readFiles(path);
            for (File file : fileList) {
                FileInputStream in = new FileInputStream(file);
                properties.load(in);
                for(Object key : properties.keySet()){
                    PropertiesCache.getInstance().getProps().putIfAbsent(key.toString(), properties.get(key).toString());
                }
            }
	        //加载完成后，启动一个FileWatchService来对文件修改状态进行监控，如果监听到文件修改，则重新加载修改后的文件内容
            addWatch(path);
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e);
		}
	}

	@Override
	public void reload(String path) {
        try{
            Properties properties = new Properties();
            //读取文件
            File file = new File(path);
            if (!file.exists()) {
                return;
            }
            FileInputStream in = new FileInputStream(file);
            properties.load(in);
            for(Object key : properties.keySet()){
                PropertiesCache.getInstance().getProps().putIfAbsent(key.toString(), properties.get(key).toString());
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e);
        }
	}

    /**
     * 添加监控
     * @param path
     */
    public void addWatch(String path) {
        FileWatchService service = new FileWatchService();
        new Thread(() -> service.addWatcher(path, this)).start();
    }

    /**
     * 读取指定文件
     * @param path
     * @return
     */
    public List<File> readFiles(String path) {
        List<File> fileList =  new ArrayList<>();
        try{
            File dir = new File(path);
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".properties")) {
                    fileList.add(file);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getStackTrace());
        }
        return fileList;
    }

}
