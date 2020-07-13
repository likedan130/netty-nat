/*
 * Copyright (c) www.bugull.com
 */
package core.utils;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public final class ByteUtil {
    
    /**
     * The left is Bit7, the right is Bit0
     * @param b
     * @param position 7-0 from left to right
     * @param value
     * @return 
     */
    public static byte setBit(byte b, int position, boolean value){
        int op = 1 << position;
        int temp = 0;
        if(value){
            temp = b | op;
        }else{
            op = ~op;
            temp = b & op;
        }
        return (byte)temp;
    }
    
    /**
     * The left is Bit7, the right is Bit0
     * @param b
     * @param position 7-0 from left to right
     * @return 
     */
    public static boolean getBit(byte b, int position){
        String s = toBinaryString(b);
        char c = s.charAt(7 - position);
        return c=='1';
    }

    public static String toBinaryString(byte b){
    	String s = Integer.toBinaryString(b & 0xFF);
        int len = s.length();
        if(len < 8){
            int offset = 8 - len;
            for(int j=0; j<offset; j++){
                s = "0" + s;
            }
        }
        return s;
    }


    public static String toHexString(byte b){
    	String s = Integer.toHexString(b & 0xFF);
        int len = s.length();
        if(len < 2){
            s = "0" + s;
        }
        return s.toUpperCase();
    }
    public static String toHexString(byte b,String prefix,String suffix){
        String s = Integer.toHexString(b & 0xFF);
        int len = s.length();
        if(len < 2){
            s = "0" + s;
        }
        s = prefix+s+suffix;
        return s.toUpperCase();
    }
    public static byte parseBinaryString(String s){
        int i = Integer.parseInt(s, 2);
        return (byte)i;
    }
    
    public static byte parseHexString(String s){
        int i = Integer.parseInt(s, 16);
        return (byte)i;
    }
    
    public static byte xor(byte... bytes){
        byte result = 0x00;
        for(byte b : bytes){
            result ^= b;
        }
        return result;
    }
    
    public static byte xor(byte[] bytes, int begin, int end){
        byte result = 0x00;
        for(int i=begin; i<end; i++){
            result ^= bytes[i];
        }
        return result;
    }
    
    public static byte sum(byte... bytes){
        byte result = 0x00;
        for(byte b : bytes){
            result += b;
        }
        return result;
    }
    
    public static byte sum(byte[] bytes, int begin, int end){
        byte result = 0x00;
        for(int i=begin; i<end; i++){
            result += bytes[i];
        }
        return result;
    }
    
    public static byte[] fromInt(int x){
        byte[] result = new byte[4];
        result[0] = (byte) ((x >> 24) & 0xFF);
        result[1] = (byte) ((x >> 16) & 0xFF);
        result[2] = (byte) ((x >> 8) & 0xFF);
        result[3] = (byte) (x & 0xFF);
        return result;
    }
    
    public static int toInt(byte[] bytes){
        int value = 0;
        for (int i=0; i<4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    public static int toInt(byte byteVal){
        return byteVal & 0xFF;
    }
    
    public static byte[] fromShort(short x){
        byte[] result = new byte[2];
        result[0] = (byte) ((x >> 8) & 0xFF);
        result[1] = (byte) (x & 0xFF);
        return result;
    }
    
    public static short toShort(byte[] bytes){
        short value = 0;
        for (int i=0; i<2; i++) {
            int shift = (2 - 1 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }
    
    public static byte[] fromLong(long x){
        byte[] result = new byte[8];
        result[0] = (byte) ((x >> 56) & 0xFF);
        result[1] = (byte) ((x >> 48) & 0xFF);
        result[2] = (byte) ((x >> 40) & 0xFF);
        result[3] = (byte) ((x >> 32) & 0xFF);
        result[4] = (byte) ((x >> 24) & 0xFF);
        result[5] = (byte) ((x >> 16) & 0xFF);
        result[6] = (byte) ((x >> 8) & 0xFF);
        result[7] = (byte) (x & 0xFF);
        return result;
    }
    
    public static long toLong(byte[] bytes){
        long value = 0;
        for (int i=0; i<8; i++) {
            int shift = (8 - 1 - i) * 8;
            value += ( (long)(bytes[i] & 0xFF) ) << shift;
        }
        return value;
    }

    public static byte[] fromFloat(float x){
        int i = Float.floatToIntBits(x);
        return fromInt(i);
    }

    public static float toFloat(byte[] bytes){
        int i = toInt(bytes);
        return Float.intBitsToFloat(i);
    }
    
    public static byte[] fromDouble(double x){
        long l = Double.doubleToRawLongBits(x);
        return fromLong(l);
    }
    
    public static double toDouble(byte[] bytes){
        long l = toLong(bytes);
        return Double.longBitsToDouble(l);
    }
    
    public static String toHexString(byte[] bytes,String prefix,String suffix){
        if(isEmpty(bytes)){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(toHexString(b,prefix,suffix));
        }
        return sb.toString();
    }
    public static String toHexString(byte[] bytes){
        if(isEmpty(bytes)){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(toHexString(b));
        }
        return sb.toString();
    }
    public static byte[] parseHexStringToArray(String s){
        if(StringUtil.isEmpty(s)){
            return null;
        }
        int len = s.length();
        if(len == 1){
        	byte[] tmp = new byte[1];
        	tmp[0] = parseHexString(s);
        	return tmp;
        }
        if(len % 2 !=0){
            return null;
        }
        int size = len / 2;
        byte[] data = new byte[size];
        for(int i=0; i<size; i++){
            String sub = s.substring(i*2, i*2+2);
            data[i] = parseHexString(sub);
        }
        return data;
    }
    
    public static boolean isEmpty(byte[] bytes){
        return bytes==null || bytes.length==0;
    }

    public static byte [] replaceAll(byte[] src,byte[] regexbt,byte[] replaceBt){
        String prefix = "";
        String suffix = ",";
        String str = ByteUtil.toHexString(src,prefix,suffix);
        String regex = ByteUtil.toHexString(regexbt,prefix,suffix);
        String replace = ByteUtil.toHexString(replaceBt,prefix,suffix);
        String afterReplace = str.replaceAll(regex,replace);
        afterReplace = afterReplace.replaceAll(suffix,"");
        return ByteUtil.parseHexStringToArray(afterReplace);

    }
    public static void main(String args[]){
//        String str = "00,0B,05,1B,01,4D,30,3E,FF,55,00,00,63,49";
//        String str1 = str.replaceAll("FF55","FF");
//        System.out.println(str);
//        System.out.println(str1);
//        byte [] src = {0x00,0x0b,0x05,0x01,(byte) 0xFF,0x55,0x67,(byte) 0xAA,(byte)0x7F,(byte)0xF5,(byte)56};
//        System.out.println(ByteUtil.toHexString(src,"0x"," "));
//        byte [] regexbt = {(byte) 0xFF,0x55};
//        byte [] replaceBt = {(byte)0xFF};
//        byte [] afterBt = ByteUtil.replaceAll(src,regexbt,replaceBt);
//        System.out.println(ByteUtil.toHexString(afterBt,"0x"," "));
        byte[] a = fromInt(10);
    }


}

