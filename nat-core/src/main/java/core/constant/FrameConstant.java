package core.constant;

public class FrameConstant {
    /**
     * 协议头
     */
    public static byte REQ_PV = (byte)0x00;
    public static byte RES_PV = (byte)0x01;

    public static byte CMD_LOGIN = (byte)0x01;

    public static byte CMD_PRE_CONNECT = (byte)0x03;

    public static byte CMD_DATA_TRANSFER = (byte)0xFF;

    public static String DEFAULT_CHANNEL_ID = "FFFFFFFF";

    /**
     * 通用成功响应
     */
    public static byte RESULT_SUCCESS = (byte)0x00;

    /**
     * 通用失败响应
     */
    public static byte RESULT_FAIL = (byte)0x01;

    /**
     * 数据帧命令字所在位置，起始位置为0
     */
    public static int FRAME_CMD_INDEX = 25;

    /**
     * 协议序号长度
     */
    public static int FRAME_SERIAL_LEN = 8;

    /**
     * 数据帧长度参数所在位置
     */
    public static int FRAME_LEN_INDEX = 26;

    /**
     * 数据帧长度参数的长度
     */
    public static int FRAME_LEN_LEN = 4;

    /**
     * 数据帧Data部分第一个字节
     */
    public static int FRAME_DTAT_FIRST_BYTE_INDEX = 12;

    /**
     * 数据帧返回结果所在位置
     */
    public static int FRAME_RESULT_INDEX = 12;

    /**
     * 数据帧密码长度所在位置
     */
    public static int FRAME_PASSWORD_LEN_INDEX = 13;

    /**
     * 数据帧返回结果参数长度
     */
    public static int FRAME_RESULT_LEN = 1;

    /**
     * 校验码长度
     */
    public static int VC_CODE_LEN = 1;

    /**
     * 数据帧最大字节数
     */
    public static int FRAME_MAX_BYTES = 655350000;

    /**
     * netty读动作空闲时间，0表示不作控制，单位秒
     */
    public static int PIPELINE_READE_TIMEOUT = 0;

    /**
     * netty读动作空闲时间，0表示不作控制，单位秒
     */
    public static int PIPELINE_READE_TIMEOUT_CONTROLL = 10;

    /**
     * netty写动作空闲时间，0表示不作控制，单位秒
     */
    public static int PIPELINE_WRITE_TIMEOUT = 0;

    /**
     * netty读写动作空闲时间，0表示不作控制，单位秒
     */
    public static int PIPELINE_READ_WRITE_TIMEOUT = 10;

    /**
     * boss线程组中线程数
     */
    public static int BOSSGROUP_NUM = 1;

    /**
     * TCP优化so_backlog参数
     */
    public static int TCP_SO_BACKLOG = 1024;

    /**
     * 内部通信channel阻止系统使用negale算法进行TCP并包发送
     */
    public static boolean TCP_NODELAY = true;

    /**
     * TCP重连尝试次数
     */
    public static int TCP_CONNECTION_RETRY_NUM = 10;

    /**
     * 当端口处于TIME_WAIT时是否允许其他程序监听该端口，用于服务重启
     */
    public static boolean TCP_REUSE_ADDR = true;

    /**
     * TCP重连尝试间隔扩大倍数
     */
    public static int TCP_RETRY_INTERVAL_ADD = 2;

    /**
     * 连接池大小的长度
     */
    public static int CHANNEL_POOL_INIT_NUM = 100;

    /**
     * 连接池大小的长度
     */
    public static int CHANNEL_POOL_NUM_LEN = 1;

    /**
     * 心跳超时时间
     */
    public static int HEARTBEAT_TIMEOUT = 15;

    /**
     * channel回收延迟，单位秒
     */
    public static int CHANNEL_RELEASE_DELAY = 2;

    public static long HEARTBEAT_INTERVAL = 10L;

    public static String DEFAULT_CHARSET = "UTF-8";

    /**
     * 数据帧最小长度
     */
    public static int FRAME_MIN_LEN = 14;


    public static int CHANNELID_LEN = 64;
}
