package core.utils;


import java.nio.ByteBuffer;

public class FrameUtils {

    private static byte pv = (byte)0xAA;

    /**
     * 拼装协议帧的工具类
     * 数据帧说明参照通信协议
     * @param datas
     * @return
     */
    public static byte[] setData(byte cmd, byte[] datas) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(datas.length + 13);
        Long serial = System.currentTimeMillis();
        //初始化校验码，真实值根据组装完成的命令进行重新计算
        byte vc = 0x00;
        short len = (short)(datas.length + 1);
        //组装命令
        byteBuffer.put(pv);
        byteBuffer.putLong(serial);
        byteBuffer.put(cmd);
        byteBuffer.putShort(len);
        byteBuffer.put(datas);
        int original = 0;
        for (byte byteVal : byteBuffer.array()) {
            original = original + (byteVal & 0xFF);
        }
        byteBuffer.put(ByteUtil.fromInt(original)[3]);
        return byteBuffer.array();
    }
}
