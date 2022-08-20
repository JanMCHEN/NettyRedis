package entry.type;

import entry.Entry;
import entry.LengthEntry;

import java.io.IOException;
import java.io.InputStream;

public class QuickListEntry implements Entry {
    private LengthEntry size;
    private ZipListEntry[] entries;
    @Override
    public int parse(InputStream in) throws IOException {
        size = new LengthEntry(true);
        size.parse(in);
        int len = size.getValue();
        entries = new ZipListEntry[len];
        for (int i=0;i<len;++i) {
            entries[i] = new ZipListEntry();
            entries[i].parse(in);
        }
        return -1;
    }
}
