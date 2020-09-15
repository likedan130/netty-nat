package core.enums;

/**
 * 字符枚举
 */
public enum StringEnum {
    LOGIN_PASSWORD("password");

    StringEnum(String value) {
        this.value = value;
    }
    private String value;

    public String getValue() {
        return value;
    }
}
