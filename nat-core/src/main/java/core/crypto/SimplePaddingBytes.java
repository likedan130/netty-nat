package core.crypto;

import java.util.Arrays;

/**
 * PaddingBytes for LSD company.
 * 
 * @author wneck130@gmail.com
 */
public class SimplePaddingBytes implements PaddingBytes {
    
    private static final int KEY_SIZE = 16;
    
    @Override
    public byte[] addPaddingBytes(byte[] plain) {
        int size = plain.length;
        int remainder = size % KEY_SIZE;
        if (remainder == 0) {
            return plain;
        } else {
            int padding = KEY_SIZE - remainder;
            byte[] result = Arrays.copyOf(plain, size + padding);
            return result;
        }
    }

    @Override
    public byte[] removePaddingBytes(byte[] plain) {
        int count = 0;
        for (int i = plain.length - 1; i >= 0; i--) {
            if (plain[i] == (byte) 0x00) {
                count++;
            } else {
                break;
            }
        }
        return Arrays.copyOfRange(plain, 0, plain.length - count);
    }

}
