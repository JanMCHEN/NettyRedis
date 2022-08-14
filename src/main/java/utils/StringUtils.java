package utils;

public class StringUtils {
    /**
     * 获取byte数组指定位的bit
     * @param b byte[]
     * @param length 可获取长度
     * @param offset 指定位置
     * @return 0/1
     */
    public static int getBit(byte[] b, int length, long offset) {
        // byte 8位, >>3获取所在的index
        int loc = (int) (offset >> 3);
        if(loc >= length){
            return 0;
        }
        offset = offset - ((long) loc << 3);
        return ((b[loc] & 0xff) >> offset) & 1;
    }

}
