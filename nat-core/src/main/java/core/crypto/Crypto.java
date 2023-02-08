package core.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.*;

/**
 * AES128 encryptor and decryptor. Code copied from bugu-droid.
 * 
 * @author wneck130@gmail.com
 */
public class Crypto {
    
    private final static Logger logger = LogManager.getLogger(Crypto.class);

    private String mAlgorithm;
    private Cipher mCipher;
    private AlgorithmParameters mAlgorithmParameters;
    private PaddingBytes mPadding;

    public Crypto(String algorithm, String blockmode) {
        mAlgorithm = algorithm;

        try {
            mAlgorithmParameters = AlgorithmParameters.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }

        try {
            String transformation = algorithm;
            if (blockmode != null) {
                transformation += "/" + blockmode;
            }
            mCipher = Cipher.getInstance(transformation);
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (NoSuchPaddingException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void setIv(byte[] iv) {
        IvParameterSpec parameterData = new IvParameterSpec(iv);
        try {
            mAlgorithmParameters.init(parameterData);
        } catch (InvalidParameterSpecException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void setPaddingBytes(PaddingBytes padding) {
        mPadding = padding;
    }

    public byte[] encrypt(byte[] plain, byte[] key, byte[] iv) {
        IvParameterSpec parameterData = new IvParameterSpec(iv);
        SecretKeySpec skeySpec = new SecretKeySpec(key, mAlgorithm);
        try {
            mCipher.init(Cipher.ENCRYPT_MODE, skeySpec, parameterData);
        } catch (InvalidKeyException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InvalidAlgorithmParameterException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return encrypt(plain);
    }

    public byte[] encrypt(byte[] plain, byte[] key) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, mAlgorithm);
        try {
            mCipher.init(Cipher.ENCRYPT_MODE, skeySpec, mAlgorithmParameters);
        } catch (InvalidKeyException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InvalidAlgorithmParameterException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return encrypt(plain);
    }

    private byte[] encrypt(byte[] plain) {
        if (mPadding != null) {
            plain = mPadding.addPaddingBytes(plain);
        }
        byte[] encrypted = null;
        try {
            encrypted = mCipher.doFinal(plain);
        } catch (IllegalBlockSizeException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (BadPaddingException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return encrypted;
    }

    public byte[] decrypt(byte[] coded, byte[] key, byte[] iv) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, mAlgorithm);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        try {
            mCipher.init(Cipher.DECRYPT_MODE, skeySpec, ivspec);
        } catch (InvalidKeyException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InvalidAlgorithmParameterException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return decrypt(coded);
    }

    public byte[] decrypt(byte[] coded, byte[] key) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, mAlgorithm);
        try {
            mCipher.init(Cipher.DECRYPT_MODE, skeySpec, mAlgorithmParameters);
        } catch (InvalidKeyException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InvalidAlgorithmParameterException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return decrypt(coded);
    }

    private byte[] decrypt(byte[] coded) {
        byte[] decrypted = null;
        try {
            decrypted = mCipher.doFinal(coded);
        } catch (IllegalBlockSizeException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (BadPaddingException ex) {
            logger.error(ex.getMessage(), ex);
        }

        if (mPadding != null) {
            decrypted = mPadding.removePaddingBytes(decrypted);
        }
        return decrypted;
    }
    
}
