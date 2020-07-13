/*
 * Copyright (c) www.bugull.com
 */

package core.crypto;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public interface PaddingBytes {
    
    public byte[] addPaddingBytes(byte[] data);

    public byte[] removePaddingBytes(byte[] data);
    
}
