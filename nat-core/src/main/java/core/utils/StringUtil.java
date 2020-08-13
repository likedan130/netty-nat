/*
 * Copyright (c) www.bugull.com
 */

package core.utils;

import io.netty.channel.Channel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public final class StringUtil {

    /**
     * Check if a string is empty
     * @param s
     * @return
     */
    public static boolean isEmpty(String s){
        return s == null || s.trim().length() == 0;
    }

    /**
     * Encrypt string s with MD5.
     * @param s
     * @return
     */
    public static String encodeMD5(String s){
        if(isEmpty(s)){
            return null;
        }
        MessageDigest md = null;
        try{
            md = MessageDigest.getInstance("MD5");
        }catch (NoSuchAlgorithmException ex) {
            //ignore ex
            return null;
        }
        char[] hexDigits = { '0', '1', '2', '3', '4',
                '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };
        md.update(s.getBytes());
        byte[] datas = md.digest();
        int len = datas.length;
        char str[] = new char[len * 2];
        int k = 0;
        for (int i = 0; i < len; i++) {
            byte byte0 = datas[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }

    public static String getArgument(String name, String[] args){
        if(args==null){
            return null;
        }
        for(int i=0; i<args.length; i++){
            String e = args[i];
            String[] ss = e.split("=");
            if(ss.length != 2){
                return null;
            }
            if(ss[0].equals(name)){
                return ss[1];
            }
        }
        return null;
    }

    public static String createRandomAesKey(){
        String src = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        int len = src.length();
        for(int i=0; i<16; i++){
            Random r = new Random();
            int index = r.nextInt(len);
            sb.append(src.charAt(index));
        }
        return sb.toString();
    }


    /**
     * 格式化json字符串输出
     * @param jsonStr
     * @return
     */
    public static String formatJson(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr))
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        char last = '\0';
        char current = '\0';
        int indent = 0;
        boolean isInQuotationMarks = false;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '"':
                    if (last != '\\'){
                        isInQuotationMarks = !isInQuotationMarks;
                    }
                    sb.append(current);
                    break;
                case '{':
                case '[':
                    sb.append(current);
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent++;
                        addIndentBlank(sb, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent--;
                        addIndentBlank(sb, indent);
                    }
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n');
                        addIndentBlank(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }

        return sb.toString();
    }

    /**
     * 添加空格
     * @param sb
     * @param indent
     */
    private static void addIndentBlank(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
        }
    }
}
