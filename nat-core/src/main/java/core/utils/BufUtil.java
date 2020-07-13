/*
 * Copyright (c) www.bugull.com
 */

package core.utils;

import io.netty.buffer.ByteBuf;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public final class BufUtil {
    
    /**
     * Note: Only use this method when it's necessary
     * @param buf
     * @return 
     */
    public static byte[] getArray(ByteBuf buf){
        byte[] result = null;
        if(buf.hasArray()){
            result = buf.array();
        }else{
            result = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), result);
        }
        return result;
    }
    
    /**
     * XOR value of all bytes in a ByteBuf
     * @param buf
     * @return 
     */
    public static byte xor(ByteBuf buf){
        byte result = 0x00;
        int len = buf.readableBytes();
        for(int i=0; i<len; i++){
            result ^= buf.getByte(i);
        }
        return result;
    }
    
    /**
     * XOR value of part bytes in a ByteBuf
     * @param buf
     * @param begin inclusive
     * @param end exclusive
     * @return 
     */
    public static byte xor(ByteBuf buf, int begin, int end){
        byte result = 0x00;
        for(int i=begin; i<end; i++){
            result ^= buf.getByte(i);
        }
        return result;
    }
    
    /**
     * sum value of all bytes in a ByteBuf
     * @param buf
     * @return 
     */
    public static byte sum(ByteBuf buf){
        byte result = 0x00;
        int len = buf.readableBytes();
        for(int i=0; i<len; i++){
            result += buf.getByte(i);
        }
        return result;
    }
    
    /**
     * sum value of part bytes in a ByteBuf
     * @param buf
     * @param begin inclusive
     * @param end exclusive
     * @return 
     */
    public static byte sum(ByteBuf buf, int begin, int end){
        byte result = 0x00;
        for(int i=begin; i<end; i++){
            result += buf.getByte(i);
        }
        return result;
    }
    
}
