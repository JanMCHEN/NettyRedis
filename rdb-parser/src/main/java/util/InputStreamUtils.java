package util;

import exception.RDBFileException;
import io.CRC64InputStream;

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
        return r & 0xff;
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
        return readWithoutEOF(in) | (readWithoutEOF(in) << 8) | (readWithoutEOF(in) << 16) | (readWithoutEOF(in) << 24);
    }

    public static int readIntBig(InputStream in) throws IOException {
        return readWithoutEOF(in) << 24 | (readWithoutEOF(in) << 16) | (readWithoutEOF(in) << 8) | readWithoutEOF(in);
    }

    public static long readLong(InputStream in) throws IOException {
        return  readInt(in) | (long) readInt(in) << 32;
    }

    public static long readLongBig(InputStream in) throws IOException {
        return (long) readIntBig(in) << 32 | readIntBig(in);
    }

    /**
     * 如果输入流不支持标记位置，简单将流封装成MarkSupportedInputStreamWrapper
     * @param in 输入流
     * @return  MarkSupportedInputStreamWrapper
     */
    public static InputStream markSupportedWrapper(InputStream in) {
        if (in.markSupported()) return in;
        return new MarkSupportedInputStreamWrapper(in);
    }

    public static InputStream CRC64InputStreamWrapper(InputStream in) {
        if (in instanceof CRC64InputStream) return in;
        return new CRC64InputStream(in);
    }

    static class MarkSupportedInputStreamWrapper extends InputStream {
        InputStream delegate;
        int mark = -1;
        boolean reset=false;
        MarkSupportedInputStreamWrapper(InputStream in) {
            delegate = in;
        }
        @Override
        public int read() throws IOException {
            if (reset) {
                reset = false;
                return mark;
            }
            mark = delegate.read();
            return mark;
        }

        @Override
        public synchronized void mark(int readLimit) {
            // only support mark first
            if(readLimit != 1) throw new IllegalArgumentException("only support mark one");
        }

        @Override
        public synchronized void reset() throws IOException {
            reset = true;
        }

        @Override
        public boolean markSupported() {
            return true;
        }
    }

}
