package core.properties.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import core.properties.cache.PropertiesCache;
import core.properties.filewatch.FileWatchService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/30
 */
@Slf4j
public class YamlLoader extends AbstractLoader {

    private static final String EXTENDTION = ".";

    @Override
    public void load(String path) throws Exception {
        //读取文件
        File propFile = readFiles(path);
        if (propFile == null) {
            throw new Exception("无法加载配置文件!!!");
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> origin = mapper.readValue(propFile, Map.class);
        unfold(origin).forEach((key, value) -> {
            PropertiesCache.getInstance().getProps().putIfAbsent(key, value);
        });
        //加载完成后，启动一个FileWatchService来对文件修改状态进行监控，如果监听到文件修改，则重新加载修改后的文件内容
        addWatch(propFile.getParent());
    }

    @Override
    public void reload(String path) throws Exception {
        //读取文件
        File propFile = readFiles(path);
        if (propFile == null) {
            throw new Exception("无法加载配置文件!!!");
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> origin = mapper.readValue(new File(path), Map.class);
        unfold(origin).forEach((key, value) -> {
            PropertiesCache.getInstance().getProps().putIfAbsent(key, value);
        });
    }

    /**
     * 添加监控
     * @param path
     */
    public void addWatch(String path) {
        FileWatchService service = new FileWatchService();
        new Thread(() -> service.addWatcher(path, this)).start();
    }

    public Map<String, Object> unfold(Map<String, Object> origin) {
        Map<String, Object> target = new HashMap<>();
        origin.forEach((key, value) -> {
            String prefix = key.toString();
            //
            if (value instanceof LinkedHashMap) {
                Map<String, Object> subMapOrigin = unfold((Map)value);
                target.putAll(subMapOrigin.entrySet().stream().collect(Collectors.toMap(
                        stringObjectEntry -> prefix+ EXTENDTION+stringObjectEntry.getKey(),
                        stringObjectEntry -> stringObjectEntry.getValue())));
            } else {
                target.put(key, value);
            }
        });
        return target;
    }

    /**
     * 读取指定路径下的文件，按文件层级由浅到深返回第一个匹配的properties.yml文件
     * @param path
     * @return
     */
    public File readFiles(String path) {
        try{
            File dir = new File(path);
            List<File> subFiles = Stream.of(dir.listFiles()).filter(File::isFile).collect(Collectors.toList());
            if (!subFiles.isEmpty()) {
                Optional<File> optional = subFiles.stream().filter((file) -> file.getName().equalsIgnoreCase("properties.yml")).findFirst();
                //当前目录下有需要的文件，直接返回
                if (optional.isPresent()) {
                    log.debug("找到配置文件：" + optional.get().getAbsolutePath());
                    return optional.get();
                }
            }
            //当前目录下没有，则查找子目录
            List<File> subDirs = Stream.of(dir.listFiles()).filter(File::isDirectory).collect(Collectors.toList());
            if (!subDirs.isEmpty()) {
                for (File subDir : subDirs) {
                    File subFile = readFiles(subDir.getPath());
                    if (subFile != null) {
                        log.debug("找到配置文件：" + subFile.getAbsolutePath());
                        return subFile;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            log.error("在路径{}下查找配置文件发生异常:", path, e);
        }
        return null;
    }
}
