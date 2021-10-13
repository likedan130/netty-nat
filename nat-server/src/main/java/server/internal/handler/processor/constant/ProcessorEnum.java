package server.internal.handler.processor.constant;

import core.netty.handler.processor.Processor;
import server.internal.handler.processor.*;

import java.util.Objects;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/24
 */
public enum ProcessorEnum {
    /**
     * 登录处理器
     */
    LOGIN((byte)0x01, LoginProcessor.class),
    /**
     * 心跳处理器
     */
    HEARTBEAT((byte)0x02, HeartbeatProcessor.class),
    /**
     * 预创建连接
     */
    PRE_CONNECT((byte)0x03, PreConnectProcessor.class),
    /**
     * 数据下发处理器
     */
    DOWN_STREAM((byte)0xEE, DownStreamProcessor.class),
    /**
     * 数据上行处理器
     */
    UP_STREAM((byte)0xFF, UpStreamProcessor.class),
    ;

    private byte cmd;

    private Class<? extends Processor> clazz;

    ProcessorEnum(byte cmd, Class<? extends Processor> clazz) {
        this.cmd = cmd;
        this.clazz = clazz;
    }

    public byte getCmd() {
        return cmd;
    }

    public Class<? extends Processor> getClazz() {
        return clazz;
    }

    /**
     * 根据cmd获取对应class对象
     * @param cmd
     * @return
     */
    public static Class<? extends Processor> getClassByCmd(byte cmd) {
        for (ProcessorEnum processorEnum : ProcessorEnum.values()) {
            if (Objects.equals(processorEnum.getCmd(), cmd)) {
                return processorEnum.getClazz();
            }
        }
        throw new EnumConstantNotPresentException(ProcessorEnum.class, cmd + "枚举常量不存在");
    }
}
