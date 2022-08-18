package entry;

import entry.type.StringEntry;
import java.io.IOException;
import java.io.InputStream;

/**
 * FA 开头的附加数据
 */
public class MetaDataEntry implements Entry{
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
        return "MetaDataEntry{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
