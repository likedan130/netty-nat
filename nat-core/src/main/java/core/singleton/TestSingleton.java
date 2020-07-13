/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.singleton;

/**
 * Project Name: xingxing-farm 
 * Package Name: core.singleton
 * ClassName: TestSingleton 
 * Function: TODO ADD FUNCTION.  
 * date: 2017/10/14 15:44
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public final class TestSingleton extends AbstractSingleton {


    /**
     * 限制性构造函数
     * 所有AbstractSingleton的子类不允许使用new关键字进行实例创建，如使用则抛出异常。
     *
     * @throws Exception
     */
    public TestSingleton() throws Exception {
    }

    public static TestSingleton getInstance() {
        return newInstance(TestSingleton.class);
    }

    public void print() {
        System.out.println(TestSingleton.class.getName());
    }
}
