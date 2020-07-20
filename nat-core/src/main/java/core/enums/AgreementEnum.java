package core.enums;

public enum AgreementEnum {
    PV((byte)0xAA,"协议头");

    private byte pv;
    private String describe;

    AgreementEnum(byte pv,String describe){
        this.pv = pv;
        this.describe = describe;
    }

    public byte getPv() {
        return pv;
    }

    public void setPv(byte pv) {
        this.pv = pv;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}
