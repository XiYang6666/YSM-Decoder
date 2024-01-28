package top.xiyang6666.ysmdecoder.ysm;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public final class AES {
    public static ByteArrayOutputStream aesEncrypt(SecretKey aesKey, AlgorithmParameterSpec iv, byte[] originalBytes) throws IOException, GeneralSecurityException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 初始化加密算法
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);

        // 流式输入数据并获取输出
        byte[] buffer = new byte[64];
        int readedLength;
        byte[] encryptedBytesTemp;

        while ((readedLength = inputStream.read(buffer)) != -1) {
            encryptedBytesTemp = cipher.update(buffer, 0, readedLength);
            if (encryptedBytesTemp != null) {
                outputStream.write(encryptedBytesTemp);
            }
        }
        encryptedBytesTemp = cipher.doFinal();
        if (encryptedBytesTemp != null) {
            outputStream.write(encryptedBytesTemp);
        }

        // 返回结果
        return outputStream;
    }

    public static ByteArrayOutputStream aesDecrypt(SecretKey aesKey, AlgorithmParameterSpec iv, byte[] encryptedBytes) throws IOException, GeneralSecurityException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 初始化加密算法
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);

        // 流式输入数据并获取输出
        byte[] buffer = new byte[64];
        int readedLength;
        byte[] decryptedBytesTemp;
        while ((readedLength = inputStream.read(buffer)) != -1) {
            decryptedBytesTemp = cipher.update(buffer, 0, readedLength);
            if (decryptedBytesTemp != null) {
                outputStream.write(decryptedBytesTemp);
            }
        }
        decryptedBytesTemp = cipher.doFinal();
        if (decryptedBytesTemp != null) {
            outputStream.write(decryptedBytesTemp);
        }

        // 返回结果
        return outputStream;
    }

    public static SecretKey createRandomAesKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }

        generator.init(128);
        return generator.generateKey();
    }

    public static SecretKey createAesKeyFromBytes(byte[] var0) {
        return new SecretKeySpec(var0, "AES");
    }

    public static IvParameterSpec createRandomIv() {
        byte[] var0 = new byte[16];
        (new SecureRandom()).nextBytes(var0);
        return new IvParameterSpec(var0);
    }
}
