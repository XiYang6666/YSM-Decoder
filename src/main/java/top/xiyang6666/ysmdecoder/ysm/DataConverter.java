package top.xiyang6666.ysmdecoder.ysm;

public final class DataConverter {
    public static byte[] intToBytes(int inputInt) {
        byte[] bytes = new byte[4];

        for (int i = 0; i < 4; ++i) {
            bytes[3 - i] = (byte) (inputInt >> 8 * i);
        }

        return bytes;
    }

    public static int bytesToInt(byte[] inputBytes, int startIndex) {
        int BYTE_LENGTH = 4;
        int result = 0;
        int endIndex = startIndex + BYTE_LENGTH;

        for (int i = startIndex; i < endIndex; ++i) {
            int byteValue = inputBytes[i] & 0xFF;
            --BYTE_LENGTH;
            byteValue <<= BYTE_LENGTH * 8;
            result += byteValue;
        }

        return result;
    }
}
