package entry.type;

import io.BytesInputStream;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * <zlbytes><zltail><zllen><entry><entry><zlend>
 *     zlbytes: 4字节整数，表示字节大小，
 *     zltail：4字节整数, 尾部即最后一个entry的偏移量，便于反向遍历
 *     zllen：2字节整数，entry数量
 *     zlend： 0xff 末尾字节
 *     entry:
 *     <length-prev-entry><special-flag><raw-bytes-of-entry>
 *
 */
public class ZipListEntry extends StringEntry{
    private int zlBytes, zlTail, zlLen;

    public void decode() throws IOException {

    }
}
