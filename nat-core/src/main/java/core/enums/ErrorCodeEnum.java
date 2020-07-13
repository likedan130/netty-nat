/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
package core.enums;

/**
 * Project Name: sanquan-farm 
 * Package Name: core.enums
 * ClassName: ErrorCodeEnum 
 * Function: TODO ADD FUNCTION.  
 * date: 2017/9/11 10:48
 * @author songwei (songw@hadlinks.com)
 * @since JDK 1.8 
 */
public enum ErrorCodeEnum {

    /*
     * 数据帧解析异常
     */
    FRAME_PARAM_MISSING("0001", "参数缺失!!!"),
    FRAME_PARAM_CAST("0002", "参数转化失败!!!"),
    FRAME_PARAM_TOO_LONG("0003", "数据帧长度超出!!!"),

    /*
     * 服务异常
     */
    SERVICE_INTERNAL_ERROR("1001", "未知的服务器内部错误!!!"),
    SERVICE_NOT_FOUND("1002", "找不到对应服务!!!"),
    SERVICE_DEV_NOT_EXIST("1003", "找不到对应设备!!!"),
    SERVICE_ILLEGAL_DATA("1004", "数据内容应用错误!!!"),
    SERVICE_INVALID_CHANNEL("1005", "数据通道未经过登陆认证!!!");


    private String errorCode;//错误码

    private String codeDoc;//错误码释义

    ErrorCodeEnum(String errorCode, String codeDoc) {}

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getCodeDoc() {
        return codeDoc;
    }

    public void setCodeDoc(String codeDoc) {
        this.codeDoc = codeDoc;
    }
}
