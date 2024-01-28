package top.xiyang6666.ysmdecoder.ysm;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class Zlib {

    /**
     * 压缩Bytes
     *
     * @param bytes 原数据
     * @return 压缩过的数据
     */
    public static byte[] deflateBytes(byte[] bytes) {
        // 判空
        if (bytes.length == 0) {
            return new byte[0];
        }

        // 压缩
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        Deflater deflater = new Deflater(9);
        deflater.setInput(bytes);
        deflater.finish();
        while (!deflater.finished()) {
            int deflatedLength = deflater.deflate(buffer);
            stream.write(buffer, 0, deflatedLength);
        }
        deflater.end();

        // 返回结果
        return stream.toByteArray();
    }

    /**
     * 解压Bytes
     *
     * @param bytes 被压缩的数据
     * @return 原数据
     * @throws DataFormatException 数据格式错误
     */
    public static byte[] decompressBytes(byte[] bytes) throws DataFormatException {
        // 判空
        if (bytes.length == 0) {
            return new byte[0];
        }

        // 解压
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        Inflater inflater = new Inflater();
        inflater.setInput(bytes, 0, bytes.length);
        while (!inflater.finished()) {
            int inflatedLength = inflater.inflate(buffer);
            stream.write(buffer, 0, inflatedLength);
        }
        inflater.end();

        // 返回结果
        return stream.toByteArray();
    }
}
