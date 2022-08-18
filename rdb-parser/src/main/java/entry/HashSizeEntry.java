package entry;

import java.io.IOException;
import java.io.InputStream;

/**
 * 可选字段，用于加速Hash空间构建
 * FB
 * $length-encoded-int         # 相应Hash表的大小
 * $length-encoded-int         # 相应带失效时间的Hash表大小
 */
public class HashSizeEntry implements Entry {
    final static int OP = 0xFB;

    private LengthEntry hashSize;
    private LengthEntry expireSize;

    @Override
    public int parse(InputStream in) throws IOException {
        hashSize = new LengthEntry();
        hashSize.parse(in);

        expireSize = new LengthEntry();
        expireSize.parse(in);
        return -1;
    }

    @Override
    public String toString() {
        return "HashSizeEntry{" +
                "hashSize=" + hashSize +
                ", expireSize=" + expireSize +
                '}';
    }
}
