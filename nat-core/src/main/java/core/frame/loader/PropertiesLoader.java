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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public void load(String path) throws Exception {
        Properties properties = new Properties();
        //读取文件
        File propFile = readFiles(path);
        if (propFile == null) {
            throw new Exception("无法加载配置文件!!!");
        }
        FileInputStream in = new FileInputStream(propFile);
        properties.load(in);
        for (Object key : properties.keySet()) {
            PropertiesCache.getInstance().getProps().putIfAbsent(key.toString(), properties.get(key).toString());
        }
        //加载完成后，启动一个FileWatchService来对文件修改状态进行监控，如果监听到文件修改，则重新加载修改后的文件内容
        addWatch(propFile.getParent());
	}

	@Override
	public void reload(String path) {
		try{
			Properties properties = new Properties();
			//读取文件
			File propFile = readFiles(path);
			if (!propFile.exists()) {
				return;
			}
			FileInputStream in = new FileInputStream(propFile);
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
	 * 读取指定路径下的文件，按文件层级由浅到深返回第一个匹配的properties文件
	 * @param path
	 * @return
	 */
	public File readFiles(String path) {
		try{
			File dir = new File(path);
            List<File> subFiles = Stream.of(dir.listFiles()).filter((file) -> file.isFile()).collect(Collectors.toList());
            if (!subFiles.isEmpty()) {
                Optional optional = subFiles.stream().filter((file) -> file.getName().endsWith("properties")).findFirst();
                //当前目录下有需要的文件，直接返回
                if (optional.isPresent()) {
                    logger.debug("找到配置文件：" + ((File) optional.get()).getAbsolutePath());
                    return (File) optional.get();
                }
            }
            //当前目录下没有，则查找子目录
            List<File> subDirs = Stream.of(dir.listFiles()).filter((file) -> file.isDirectory()).collect(Collectors.toList());
            if (!subDirs.isEmpty()) {
                for (File subDir : subDirs) {
                    File subFile = readFiles(subDir.getPath());
                    if (subFile != null) {
                        logger.debug("找到配置文件：" + subFile.getAbsolutePath());
                        return subFile;
                    }
                }
            }
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

}
