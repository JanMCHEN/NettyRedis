package entry.type;

import entry.DoubleEntry;
import entry.Entry;
import entry.LengthEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ZsetEntry implements Entry {
    private LengthEntry size;
    private ValueScoreEntry[] entries;

    @Override
    public int parse(InputStream in) throws IOException {
        size = new LengthEntry(true);
        size.parse(in);
        int len = size.getValue();
        entries = new ValueScoreEntry[len];
        for (int i=0;i<len;++i) {
            ValueScoreEntry child = new ValueScoreEntry();
            child.parse(in);
            entries[i] = child;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "ZsetEntry{" +
                "entries=" + Arrays.toString(entries) +
                '}';
    }

    static class ValueScoreEntry implements Entry {
        private StringEntry value;
        private DoubleEntry score;
        @Override
        public int parse(InputStream in) throws IOException {
            value = new StringEntry();
            value.parse(in);
            score = new DoubleEntry();
            score.parse(in);
            return -1;
        }

        @Override
        public String toString() {
            return "(value=" + value +
                    ", score=" + score +
                    ')';
        }
    }
}
