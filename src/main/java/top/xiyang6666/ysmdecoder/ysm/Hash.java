package top.xiyang6666.ysmdecoder.ysm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hash {
    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException var1) {
            throw new RuntimeException(var1);
        }
    }

    public static byte[] md5Hash(byte[] var0) {
        return MESSAGE_DIGEST.digest(var0);
    }
}
