package entry.type;

import entry.Entry;
import entry.LengthEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ListEntry implements Entry {
    private LengthEntry size;
    private List<StringEntry> entries;
    @Override
    public int parse(InputStream in) throws IOException {
        size = new LengthEntry(true);
        size.parse(in);
        int length = size.getValue();
        entries = new ArrayList<>();
        for (int i=0;i<length;++i) {
            StringEntry entry = new StringEntry();
            entry.parse(in);
            entries.add(entry);
        }
        return -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "entries=" + entries +
                '}';
    }
}
