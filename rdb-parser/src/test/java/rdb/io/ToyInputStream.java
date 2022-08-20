package rdb.io;

import java.io.IOException;
import java.io.InputStream;

public class ToyInputStream extends InputStream {
    int readIndex;
    byte[] buffer;
    @Override
    public int read() throws IOException {
        if(readIndex >= buffer.length) return -1;
        return buffer[readIndex++] & 0xff;
    }

    public void fromHexString(String s) {
        s = s.toLowerCase();
        int n = s.length();
        byte[] b = new byte[n];
        int j = 0;
        for(int i=0;i<n;++i) {
            char c = s.charAt(i);
            if (c >= '0' && c <='9') {
                b[j++] = (byte)(c-'0');
            }
            else if(c >= 'a' && c <='z') {
                b[j++] = (byte)(c-'a'+10);
            }
        }

        buffer = new byte[j/2];
        for (int i=0;i<buffer.length;++i) {
            buffer[i] = (byte) (b[i*2]<<4 | b[i*2+1]);
        }
        readIndex = 0;
    }

    public static void main(String[] args) throws IOException {
        ToyInputStream toyInputStream = new ToyInputStream();
        toyInputStream.fromHexString("04\n" +
                "01 63 12 34 2E 30 31 39 39 39 39 39 39 39 39 39 39 39 39 39 36\n" +
                "01 64 FE\n" +
                "01 61 12 33 2E 31 38 39 39 39 39 39 39 39 39 39 39 39 39 39 39\n" +
                "01 65 FF");

        System.out.println(Long.toHexString(toyInputStream.read()));
    }
}
