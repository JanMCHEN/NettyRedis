package util;

import exception.RDBFileException;

import java.io.IOException;
import java.io.InputStream;

/**
 * little endian
 */
public class InputStreamUtils {
    public static int readWithoutEOF(InputStream in) throws IOException {
        int r = in.read();
        if(r == -1) {
            throw new RDBFileException("wrong end");
        }
        return r;
    }

    public static byte[] readNBytes(int n, InputStream in) throws IOException {
        byte[] bytes = new byte[n];
        int read = in.readNBytes(bytes, 0, n);
        if (read < n) {
            throw new RDBFileException("wrong end");
        }
        return bytes;
    }

    public static int readInt(InputStream in) throws IOException {
        return readWithoutEOF(in) | (readWithoutEOF(in) << 4) | (readWithoutEOF(in) << 8) | (readWithoutEOF(in) << 16);
    }

    public static long readLong(InputStream in) throws IOException {
        return  readInt(in) | (long) readInt(in) << 32;
    }

}
