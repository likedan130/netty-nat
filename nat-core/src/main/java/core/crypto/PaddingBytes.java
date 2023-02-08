package core.crypto;

/**
 *
 * @author wneck130@gmail.com
 */
public interface PaddingBytes {
    
    public byte[] addPaddingBytes(byte[] data);

    public byte[] removePaddingBytes(byte[] data);
    
}
