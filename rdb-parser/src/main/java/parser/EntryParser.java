package parser;

import entry.Entry;
import entry.LengthEntry;

import java.io.IOException;
import java.io.InputStream;

public interface EntryParser <T extends Entry>{
    default int parse(int op, InputStream in) throws IOException {
        if(!canParse(op)) return op;
        return parse(in);
    }
    boolean canParse(int op);
    int parse(InputStream in);

    class LengthEntryParser implements EntryParser<LengthEntry> {
        @Override
        public boolean canParse(int op) {
            return false;
        }

        @Override
        public int parse(InputStream in) {
            return 0;
        }
    }
}
