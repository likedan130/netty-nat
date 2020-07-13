/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.enums;

/**
 * Project Name: bugu-farm-household
 * Package Name: core.enums
 * ClassName: BuguEventEnum 
 * Function: TODO ADD FUNCTION.  
 * date: 2017/10/30 14:45
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public enum BuguEventEnum {

    EVENT_TYPE_CMD_RESPONSE(1, "服务器主动下发命令收到响应事件"),
    EVENT_TYPE_CMD_TIMEOUT(2, "服务器主动下发命令超时未收到响应事件"),
    EVENT_TYPE_CMD_EXCEPTION(3, "服务器主动下发命令时发生异常");

    BuguEventEnum(int type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }

    private int type;

    private String typeName;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
