/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Project Name: xingxing-farm 
 * Package Name: core.singleton
 * ClassName: AbstractSingleton 
 * Function: 项目中所有单例对象的统一父类，模拟懒加载的形式，只在首次调用时创建对象.
 * date: 2017/10/14 14:41
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public abstract class AbstractSingleton implements Serializable {

    private static final ConcurrentMap<String, AbstractSingleton> classMap = new ConcurrentHashMap<>();
    private static final long serialVersionUID = -8691344637857430181L;
    private static final Logger logger = LogManager.getLogger(AbstractSingleton.class);

    /**
     * 限制性构造函数
     * 所有AbstractSingleton的子类除首次实例创建外，不允许使用new关键字进行实例创建，如在实例已经存在时使用则抛出异常。
     * 并发情况下，锁表后进行进一步判断，防止多线程创建时出现问题。
     * @throws Exception
     */
    AbstractSingleton () throws Exception {
        String clazzName = this.getClass().getName();
        if (classMap.containsKey(clazzName)) {
            throw new Exception("Cannot construct instance for singleton class " + clazzName
                    + ", cause an instance has existed !");
        } else {
            synchronized (classMap) {
                if (classMap.containsKey(clazzName)) {
                    throw new Exception("Cannot construct instance for singleton class " + clazzName
                            + ", cause an instance has existed !");
                } else {
                    classMap.putIfAbsent(clazzName, this);
                }
            }
        }
    }

    /**
     * 返回一个AbstractSingleton子类的单例对象实例
     * 如果首次调用，则创建实例并缓存在classMap中，其余时候调用均返回classMap中缓存的实例对象，模拟懒汉模式
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T extends AbstractSingleton> T newInstance(Class<T> clazz) {
        String className = clazz.getName();
        if (!classMap.containsKey(className)) {
            try {
                T instatnce = clazz.newInstance();
                classMap.putIfAbsent(clazz.getName(), instatnce);
                return instatnce;
            } catch (Exception e) {
                logger.error(e);
                return null;
            }
        } else {
            return (T)classMap.get(className);
        }
    }
}
