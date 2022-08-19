package util;

import java.util.Arrays;
import java.util.zip.Deflater;

public class LZF_Utils {
    public static void main(String[] args) {
        byte[] in = {0x06, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x68,  0x20, 0x05, 0x01, 0x6f, 0x20};

        System.out.println(new String(lzfDecode(in, 12)));
    }

    public static byte[] lzfDecode(byte[] b, int len) {
        byte[] out = new byte[len];
        int x = 0, y = 0;
        while (x < b.length) {
            int ctrl = b[x++] & 0xff;
            if (ctrl < 32) {
                for (int i=0; i <= ctrl; ++i) {
                    out[y++] = b[x++];
                }
            } else {
                int sz = ctrl >> 5;
                if (sz == 7) {
                    sz += (b[x++] & 0xff);
                }
                int ref = y - ((ctrl & 0x1f) << 8) - b[x++] - 1;
                for (int i = 0; i < sz + 2; ++i) {
                    out[y++] = out[ref++];
                }
            }
        }
        if(y!=len) {
            throw new IllegalArgumentException("not a lzf input");
        }
        return out;
    }
}
