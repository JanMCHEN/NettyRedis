package entry;

import entry.type.StringEntry;
import java.io.IOException;
import java.io.InputStream;

/**
 * hash 中的节点
 * key:value
 */
public class HashNodeEntry implements Entry{
    private StringEntry key, value;

    @Override
    public int parse(InputStream in) throws IOException {
        key = new StringEntry();
        key.parse(in);
        value = new StringEntry();
        value.parse(in);
        return -1;
    }

    @Override
    public String toString() {
        return "(key=" + key +
                ", value=" + value +
                ')';
    }
}
