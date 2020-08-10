package core.detection;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.ArrayUtil;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

/**
 * @author xian
 * date:2020/07/20
 * 是否遵循自定义协议检测
 */
public class PublicDetectionHandler {

    public static boolean detection(ByteBuf msg) throws Exception{
        //获取总长度
        int length = msg.readableBytes();
        //可读长度不够，说明不符合协议，不做解析
        if (length < 12) {
            return true;
        }
        //获取协议头
        byte pv = msg.getByte(0);
        //获取序列号
        long serial = msg.getLong(1);
        //获取命令
        byte cmd = msg.getByte(9);
        //获取DATA长度
        short len = msg.getShort(10);
        //判断协议头
        if (pv != FrameConstant.pv){
            return true;
        }
        //自定义命令数组
        byte[] cmdSet = new byte[]{CommandEnum.CMD_LOGIN.getCmd(),CommandEnum.CMD_HEARTBEAT.getCmd(),CommandEnum.CMD_CONNECTION_POOL.getCmd()};
        //判断是否包含命令
        if(!ArrayUtil.contains(cmdSet,cmd)){
            return true;
        };
        //接入命令
        if(cmd == CommandEnum.CMD_LOGIN.getCmd()){
            int vc = msg.getByte(21);
         }
        //心跳命令
        if(cmd == CommandEnum.CMD_HEARTBEAT.getCmd()){

        }
        //连接池命令
        if(cmd == CommandEnum.CMD_CONNECTION_POOL.getCmd()){

        }
        return false;
    }
}
