package entry.type;

import entry.Entry;
import entry.HashNodeEntry;
import entry.LengthEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class HashEntry implements Entry {
    private LengthEntry size;
    private HashNodeEntry[] entries;
    @Override
    public int parse(InputStream in) throws IOException {
        size = new LengthEntry(true);
        size.parse(in);

        int len = size.getValue();
        entries = new HashNodeEntry[len];

        for (int i=0;i<len;++i) {
            HashNodeEntry kv = new HashNodeEntry();
            kv.parse(in);
            entries[i] = kv;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "HashEntry{" +
                "entries=" + Arrays.toString(entries) +
                '}';
    }
}
