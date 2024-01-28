package top.xiyang6666.ysmdecoder.ysm;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.zip.DataFormatException;

public final class YSMFile {
    private static final Path exportPath = Path.of("./");

    public static Map<String, byte[]> loadYsmFile(File ysmFile) throws IOException {
        // 读文件数据
        byte[] fileBytes = FileUtils.readFileToByteArray(ysmFile);

        // 读校验码和版本
        int checkCode = DataConverter.bytesToInt(fileBytes, 0);
        int version = DataConverter.bytesToInt(fileBytes, 4);
        if (checkCode != 1498629968) return Collections.emptyMap();// 校验校验码
        if (version != 1 && version != 2) return Collections.emptyMap();// 校验版本


        // 读取数据,并哈希校验
        byte[] hash = ByteArrays.copy(fileBytes, 8, 16); // 读哈希
        byte[] data = ByteArrays.copy(fileBytes, 24, fileBytes.length - 24);// 读数据
        if (!Arrays.equals(hash, Hash.md5Hash(data))) {
            return Collections.emptyMap();
        }

        // 从数据加载文件
        HashMap<String, byte[]> result = Maps.newHashMap();
        ByteArrayInputStream fileStream = new ByteArrayInputStream(data);
        while (fileStream.available() > 0) {
            try {
                // 解码文件
                Pair<String, byte[]> loadFileResult;
                if (version == 1) loadFileResult = loadFileV1(fileStream);
                else loadFileResult = loadFileV2(fileStream);
                result.put(loadFileResult.left(), loadFileResult.right());
            } catch (DataFormatException | GeneralSecurityException var10) {
                var10.printStackTrace();
            }
        }

        // 返回解码结果
        return result;
    }

    private static @NotNull Pair<String, byte[]> loadFileV1(ByteArrayInputStream var0) throws IOException, GeneralSecurityException, DataFormatException {
        String var1 = readFileName(var0);
        int var2 = readInt(var0);
        byte[] var3 = new byte[16];
        byte[] var4 = new byte[16];
        var0.read(var3);
        var0.read(var4);
        SecretKeySpec var5 = new SecretKeySpec(var3, "AES");
        IvParameterSpec var6 = new IvParameterSpec(var4);
        byte[] var7 = new byte[var2];
        var0.read(var7);
        ByteArrayOutputStream var8 = AES.aesDecrypt(var5, var6, var7);
        byte[] var9 = Zlib.decompressBytes(var8.toByteArray());
        return Pair.of(var1, var9);
    }

    private static @NotNull Pair<String, byte[]> loadFileV2(ByteArrayInputStream stream) throws IOException, GeneralSecurityException, DataFormatException {
        String fileName = readB64FileName(stream);

        // 读取文件数据和随机秘钥
        int dataLength = readInt(stream);
        int randomKeyLength = readInt(stream);
        byte[] encryptedRandomAesKeyBytes = new byte[randomKeyLength];
        byte[] IvBytes = new byte[16];
        byte[] encryptedData = new byte[dataLength];
        stream.read(encryptedRandomAesKeyBytes);
        stream.read(IvBytes);
        stream.read(encryptedData);

        // 构造iv
        IvParameterSpec ivParameterSpec = new IvParameterSpec(IvBytes);
        // 生成哈希密钥
        byte[] hashAesKeyByte = generateRandomBytes(encryptedData);
        SecretKey hashAesKey = AES.createAesKeyFromBytes(hashAesKeyByte);
        // 解密随机秘钥
        byte[] randomAesKeyBytes = AES.aesDecrypt(hashAesKey, ivParameterSpec, encryptedRandomAesKeyBytes).toByteArray();
        SecretKey randomAesKey = AES.createAesKeyFromBytes(randomAesKeyBytes);
        // 解密数据
        ByteArrayOutputStream deflateDataStream = AES.aesDecrypt(randomAesKey, ivParameterSpec, encryptedData);
        // 解压数据
        byte[] dataBytes = Zlib.decompressBytes(deflateDataStream.toByteArray());

        // 返回结果
        return Pair.of(fileName, dataBytes);
    }

    public static void saveYsmFile(File file) throws IOException {
        String fileName = file.getName();
        Collection<File> fileCollection = FileUtils.listFiles(file, FileFileFilter.INSTANCE, null);
        Iterator<File> iterator = fileCollection.iterator();

        // 检测必要文件
        boolean noMainFile = true;
        boolean noArmFile = true;
        boolean noPngFile = true;
        File necessaryFile;
        while (iterator.hasNext()) {
            necessaryFile = iterator.next();
            String file1Name = necessaryFile.getName();
            if ("main.json".equals(file1Name)) noMainFile = false;
            if ("arm.json".equals(file1Name)) noArmFile = false;
            if (file1Name.endsWith(".png")) noPngFile = false;
        }
        if (!(noMainFile && noArmFile && noPngFile)) return;

        // 编码文件
        byte[] encryptedData = createYsmFile(fileCollection);
        // 保存文件
        necessaryFile = exportPath.resolve(fileName + ".ysm").toFile();
        FileUtils.writeByteArrayToFile(necessaryFile, encryptedData, false);

    }

    private static byte[] createYsmFile(Collection<File> fileCollection) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // 写入版本和校验码
        stream.write(DataConverter.intToBytes(1498629968));
        stream.write(DataConverter.intToBytes(2));
        // 加密文件,写入数据与哈希值
        byte[] encryptedData = encryptFileCollection(fileCollection);
        byte[] encryptedDataHash = Hash.md5Hash(encryptedData);
        stream.write(encryptedDataHash);
        stream.write(encryptedData);
        // 返回结果
        return stream.toByteArray();
    }

    private static byte[] encryptFileCollection(Collection<File> fileCollection) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // 逐个加密文件
        fileCollection.forEach((file) -> {
            try {
                stream.write(encryptFile(file));
            } catch (GeneralSecurityException | IOException exception) {
                exception.printStackTrace();
            }
        });
        // 返回结果
        return stream.toByteArray();
    }

    private static byte[] encryptFile(File file) throws IOException, GeneralSecurityException {
        // 压缩文件
        byte[] fileBytes = FileUtils.readFileToByteArray(file);
        byte[] deflatedFileBytes = Zlib.deflateBytes(fileBytes);

        // 生成随机秘钥和iv
        SecretKey randomAesKey = AES.createRandomAesKey();
        IvParameterSpec ivParameterSpec = AES.createRandomIv();
        // 加密数据
        ByteArrayOutputStream encryptedFileData = AES.aesEncrypt(randomAesKey, ivParameterSpec, deflatedFileBytes);
        // 从加密过的文件数据的摘要生成秘钥
        byte[] hashAesKeyByte = generateRandomBytes(encryptedFileData.toByteArray());
        SecretKey hashAesKey = AES.createAesKeyFromBytes(hashAesKeyByte);
        // 加密秘钥
        ByteArrayOutputStream encryptedRandomAesKey = AES.aesEncrypt(hashAesKey, ivParameterSpec, randomAesKey.getEncoded());

        // 写入数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeB64FileName(outputStream, file.getName()); // 写入文件名长度及文件名
        outputStream.write(DataConverter.intToBytes(encryptedFileData.size())); // 写入加密后数据长度
        outputStream.write(DataConverter.intToBytes(encryptedRandomAesKey.size())); // 写入加密后秘钥长度
        outputStream.write(encryptedRandomAesKey.toByteArray()); // 写入秘钥
        outputStream.write(ivParameterSpec.getIV()); // 写入IV
        outputStream.write(encryptedFileData.toByteArray()); // 写入数据
        return outputStream.toByteArray(); // 返回结果
    }

    private static byte[] generateRandomBytes(byte[] var0) {
        byte[] var1 = Hash.md5Hash(var0);
        Random var2 = new Random(byteArrayToLong(var1));
        byte[] var3 = new byte[16];
        var2.nextBytes(var3);
        return var3;
    }

    private static long byteArrayToLong(byte[] input) {
        long result = 0L;
        byte[] array = input;
        int arrayLength = input.length;

        for (int i = 0; i < arrayLength; ++i) {
            byte temp = array[i];
            result = (result << 8) + (long) (temp & 255);
        }

        return result;
    }

    private static void writeFileName(ByteArrayOutputStream stream, String fileName) throws IOException {
        byte[] var2 = fileName.getBytes(StandardCharsets.UTF_8);
        stream.write(DataConverter.intToBytes(var2.length));
        stream.write(var2);
    }

    private static void writeB64FileName(ByteArrayOutputStream stream, String fileName) throws IOException {
        byte[] var2 = Base64.getEncoder().encode(fileName.getBytes(StandardCharsets.UTF_8));
        stream.write(DataConverter.intToBytes(var2.length));
        stream.write(var2);
    }

    private static String readFileName(ByteArrayInputStream fileStream) throws IOException {
        int fileNameLength = readInt(fileStream);
        byte[] fileNameBytes = new byte[fileNameLength];
        fileStream.read(fileNameBytes);
        return new String(fileNameBytes, StandardCharsets.UTF_8);
    }

    private static String readB64FileName(ByteArrayInputStream fileStream) throws IOException {
        int fileNameLength = readInt(fileStream);
        byte[] b64fileName = new byte[fileNameLength];
        fileStream.read(b64fileName);
        byte[] fileNameBytes = Base64.getDecoder().decode(b64fileName);
        return new String(fileNameBytes, StandardCharsets.UTF_8);
    }

    private static int readInt(ByteArrayInputStream stream) throws IOException {
        byte[] intBytes = new byte[4];
        stream.read(intBytes);
        return DataConverter.bytesToInt(intBytes, 0);
    }
}
