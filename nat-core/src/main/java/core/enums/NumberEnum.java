package core.enums;

/**
 * @author xian
 * Created by Administrator on 2020/7/7.
 */
public enum NumberEnum {
    ZERO(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    FORTYFIVE(45),
    FIFTEEN(15),
    TWELVE(12),
    FOURTEEN(14),
    ONETWOFIVE(125),
    TWOHUNDREDANDFIFTYSIX(256),
    FIVEHUNDRED(500),
    ONETHOUSAND(1000),
    ONETHOUSANDANDTWENTYFOUR(1024),
    TWOTHOUSANDANDFORTYEIGHT(2048),
    SIXTYFIVETHOUSANDFIVEHUNDREDANDTHIRTYSIX(65536),
    FIVETHOUSAND(5000),
    TENTHOUSAND(10000),
    ONEHUNDRED(100),
    FIFTY(50),
    FORTYNINE(49),
    ONEZEROSIX(106),
    TWO_THOUSAND(2000),
    TWO_HUNDRED(200);


    private int value;

    NumberEnum(Integer value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
