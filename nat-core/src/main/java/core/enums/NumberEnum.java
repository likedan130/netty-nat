package core.enums;

/**
 * 数字枚举
 */
public enum NumberEnum {

    HEART_TATE_INTERVAL(15,"心跳间隔时间");

    NumberEnum(int type, String typeName) {
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
