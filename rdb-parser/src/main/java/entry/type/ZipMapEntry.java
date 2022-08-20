package entry.type;

import entry.Entry;
import exception.RDBFileException;
import io.BytesInputStream;
import util.InputStreamUtils;
import io.NoCopyCharSequence;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <zmlen><len>"foo"<len><free>"bar"<len>"hello"<len><free>"world"<zmend>
 *  zmlen: 1 byte that holds the size of the zip map. If it is greater than or equal to 254, value is not used. You will have to iterate the entire zip map to find the length.
 *  len: the length of the following string, which can be either a key or a value. This length is stored in either 1 byte or 5 bytes (yes, it differs from Length Encoding described above).
 * If the first byte is between 0 and 252, that is the length of the zipmap. If the first byte is 253, then the next 4 bytes read as an unsigned integer represent the length of the zipmap. 254 and 255 are invalid values for this field.
 *  free : This is always 1 byte, and indicates the number of free bytes after the value. For example, if the value of a key is “America” and its get updated to “USA”, 4 free bytes will be available.
 *  zmend : Always 255. Indicates the end of the zipmap.
 *
 *  StringEntry解码得到字符串
 * 先读取一个字节的长度，如果>=254，实际长度需要遍历得知 ；
 */
public class ZipMapEntry extends StringEntry {
    public ZipMapEntry() {}

    /**
     * 解码zipMap
     * <zmlen><len>"foo"<len><free>"bar"<len>"hello"<len><free>"world"<zmend>
     */
    public List<ZipMapNodeEntry> decode() throws IOException {
        InputStream in = new BytesInputStream(getBytes());
        int zmLen = InputStreamUtils.readWithoutEOF(in);
        List<ZipMapNodeEntry> entries = new ArrayList<>(zmLen);
        while (true) {
            ZipMapNodeEntry node = new ZipMapNodeEntry();
            int ret = node.parse(in);
            if(ret == 0xff) break;
            entries.add(node);
        }
        return entries;
    }

    @Override
    public String toString() {
        List<ZipMapNodeEntry> entries;
        try {
            entries = decode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "ZipMapEntry{" +
                "entries=" + entries +
                '}';
    }

    /**
     *
     * 读取一个byte，0-252表示长度，253则由后面4个字节表示长度，255代表zipMap结束
     */
    static class ZipMapNodeEntry implements Entry {
        private int keyLen, valLen, free;
        private CharSequence key, value;

        @Override
        public int parse(InputStream in) throws IOException {
            int b = InputStreamUtils.readWithoutEOF(in);
            if (b == 0xff) {
                // end
                return b;
            }
            keyLen = parseLength(b, in);
            byte[] bytes = InputStreamUtils.readNBytes(keyLen, in);
            key = new NoCopyCharSequence(bytes);

            b = InputStreamUtils.readWithoutEOF(in);
            valLen = parseLength(b, in);
            free = InputStreamUtils.readWithoutEOF(in);
            value = new NoCopyCharSequence(InputStreamUtils.readNBytes(valLen, in));
            return -1;
        }

        private int parseLength(int b, InputStream in) throws IOException {
            if (b == 253) {
                b = InputStreamUtils.readInt(in);
            } else if (b > 253) {
                throw new RDBFileException("zip_map非法长度");
            }
            return b;
        }

        @Override
        public String toString() {
            return "(key=" + key + ",value=" + value + ")";
        }
    }
}
