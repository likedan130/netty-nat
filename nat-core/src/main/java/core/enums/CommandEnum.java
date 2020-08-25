/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.enums;

import core.utils.ByteUtil;

/**
 * Project Name: bugu-farm-household
 * Package Name: core.enums
 * ClassName: CommandEnum 
 * Function: TODO ADD FUNCTION.  
 * date: 2017/7/7 14:05
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public enum CommandEnum {

    //长连接设备相关指令（机械/电控设备）
    CMD_LOGIN((byte)0x01, "接入命令"),
    CMD_HEARTBEAT((byte)0x02, "心跳命令"),
    CMD_START_PROXY_CLIENT((byte)0x03, "启动代理客户端"),
    CMD_CHANNEL_RECYCLE((byte)0x04, "连接回收"),
    CMD_DATA_TRANSFER((byte)0xFF, "业务数据转发");


    private byte cmd;//通信协议命令字

    private String chName;//命令释义

    CommandEnum(byte cmd, String chName) {
        this.cmd = cmd;
        this.chName = chName;
    }

    public byte getCmd () {
        return this.cmd;
    }

    public String getHexCmd () {
        return toHexString(this.cmd);
    }

    public String getChName() {
        return chName;
    }

    /**
     * 根据cmd的值获得一个CommandEnum实例
     * @param cmd
     * @return CommandEnum
     * @throws Exception
     */
    public static CommandEnum getCommandEnumByCmd(String cmd) throws Exception {
        byte cmdByte = ByteUtil.parseHexString(cmd);
        for (CommandEnum constant : CommandEnum.values()) {
            if (constant.getCmd() == cmdByte) {
                return constant;
            }
        }
        throw new Exception("Can't find the specific CommandEnum instance for input param : " + cmd);
    }

    /**
     * 根据cmd的值获得一个CommandEnum实例
     * @param cmd
     * @return
     * @throws Exception
     */
    public static CommandEnum getCommandEnumByCmd(byte cmd) throws Exception {
        for (CommandEnum constant : CommandEnum.values()) {
            if (constant.getCmd() == cmd) {
                return constant;
            }
        }
        throw new Exception("Can't find the specific CommandEnum instance for input param : " + cmd);
    }

    /**
     * 根据枚举名称获取一个CommandEnum实例
     * @param name
     * @return
     * @throws Exception
     */
    public static CommandEnum getCommandEnumByName (String name) throws Exception {
        return Enum.valueOf(CommandEnum.class, name);
    }

    /**
     * 根据枚举名称或cmd获取一个CommandEnum实例
     * @param param
     * @return
     * @throws Exception
     */
    public static CommandEnum getCommandEnum (String param) throws Exception {
        try {
            return getCommandEnumByName(param);
        } catch (Exception e) {
            return getCommandEnumByCmd(Integer.valueOf(param).byteValue());
        }
    }
    private String toHexString(byte b){
        String s = Integer.toHexString(b & 0xFF);
        int len = s.length();
        if(len < 2){
            s = "0" + s;
        }
        return s.toUpperCase();
    }
}
