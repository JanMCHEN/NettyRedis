package entry.type;

import entry.Entry;

import java.io.IOException;
import java.io.InputStream;

public interface TypeEntry extends Entry {
    boolean isType(int op);
    int parse(InputStream in);
}
