package core.utils;

/**
 * @author xian
 * 2020/07/20
 * 数组工具类
 */
public class ArrayUtil {

    /**
     * 数组是否包含某元素
     * @param array
     * @param value
     * @return
     */
    public static boolean contains(byte[] array, byte value) {
        return indexOf(array, value) > -1;
    }

    /**
     * 循环遍历
     * @param array
     * @param value
     * @return
     */
    public static int indexOf(byte[] array, byte value) {
        if (null != array) {
            for(int i = 0; i < array.length; ++i) {
                if (value == array[i]) {
                    return i;
                }
            }
        }
        return -1;
    }
}
