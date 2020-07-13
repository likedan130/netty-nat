package core.enums;

/**
 * 用途:消息枚举
 *
 * @author loumt(loumt@hadlinks.com)
 */
public enum MessageEnum {

    DEFAULT(0,"默认消息"),
    //后台跑出去的消息
    NOTICE(1,"广告通知"),
    //第三方消息
    PUSH(2,"推送子用户"),
    //用户之间的消息
    FAULT(3,"故障消息");

    private int code;
    private String desc;

    MessageEnum(int code,String desc) {
        this.code=code;
        this.desc=desc;
    }

    public String desc(){
        return this.desc;
    }
    public int code(){return this.code;}

    public String desc(int code){
        for (MessageEnum messageEnum:MessageEnum.values()){
            if(messageEnum.code==code){
                return this.desc;
            }
        }
        return null;
    }
}
