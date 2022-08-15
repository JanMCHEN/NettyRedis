package utils;

import core.structure.RedisIntString;
import core.structure.RedisSimpleDynamicString;

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

    public static long bitCount(final CharSequence cs) {
        long ans = 0;
        int cur, n = cs.length();
        for(int i=0;i<n;++i) {
            cur = cs.charAt(i) & 0xffff;
            while (cur>0){
                cur &= (cur-1);
                ans ++;
            }
        }
        return ans;
    }

    public static byte[] getBytes(final CharSequence contents) {
        if(contents instanceof String) {
            return ((String) contents).getBytes();
        }
        if (contents instanceof RedisSimpleDynamicString) {
            return ((RedisSimpleDynamicString) contents).getBytes();
        }
        if (contents instanceof RedisIntString) {
            return ((RedisIntString) contents).getBytes();
        }
        int n = contents.length();
        byte[] res = new byte[n*2];
        for (int i=0;i<n;++i) {
            char c = contents.charAt(i);
            res[i*2] = (byte)(c >> 8);
            res[i*2+1] = (byte) (c & 0xff);
        }
        return res;
    }

}
