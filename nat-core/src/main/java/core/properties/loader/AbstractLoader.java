package core.properties.loader;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ClassName: AbstructLoader 
 * Function: 文件加载器，可以用于加载配置文件(.properties)或通信协议模板文件(.xml).  
 * date: 2017年3月20日 上午10:43:14  
 * 
 * @author wneck130@gmail.com
 * @version 
 * @since JDK 1.8 
 */
public abstract class AbstractLoader {

	/**
	 * 加载配置文件的名称
	 */
	protected String[] targetFilenames;

	public AbstractLoader(String... targetFilenames) {
		this.targetFilenames = targetFilenames;
	}

	/**
	 * 加载文件，将文件内容缓存入内存中供程序使用
	 */
	public abstract void load(String path) throws Exception;
	
	/**
	 * 重载文件，当需要实时监测文件的修改状态时，实现本方法
	 * 可以在文件修改事件触发的时候重载文件，刷新缓存在内存中的文件内容
	 */
	public abstract void reload(String path) throws Exception;

	public boolean match(String filename) {
		return Arrays.stream(targetFilenames).anyMatch(targetFilename -> Pattern.matches(targetFilename, filename));
	}

  public static void main(String[] args) {
	  String filename = "properties.yml";
	  String[] targetFilenames = new String[]{"properties.yml", "properties.properties"};
	  System.out.println(Arrays.stream(targetFilenames).anyMatch(targetFilename -> Pattern.matches(targetFilename, filename)));
  }
}
