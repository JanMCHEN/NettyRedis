package entry;

import entry.type.ListEntry;
import entry.type.StringEntry;
import exception.RDBFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class KeyValueEntry implements Entry{
    private static final Logger log = LoggerFactory.getLogger(KeyValueEntry.class);
    private final static Map<Integer, Supplier<Entry>> typeEntries = new HashMap<>(16, 1.f);
    static {
        typeEntries.put(0, StringEntry::new);
        typeEntries.put(1, ListEntry::new);
//        typeEntries.put()
    }
    private ExpireEntry expireEntry;
    private Entry valueEntry;
    private StringEntry keyEntry;

    @Override
    public int parse(InputStream in) throws IOException {
        int op = InputStreamUtils.readWithoutEOF(in);
        expireEntry = ExpireEntry.getExpireEntry(op);
        if (expireEntry != null) {
            expireEntry.parse(in);
            op = InputStreamUtils.readWithoutEOF(in);
        }

        if (expireEntry==null && (op==EOF)||(op==DB_SELECT_OP)) {
            log.warn("empty key-value-entry");
            return op;
        }

        Supplier<Entry> entrySupplier = typeEntries.get(op);
        if(entrySupplier==null) {
            throw new RDBFileException("cant parse type: "+ Integer.toHexString(op));
        }
        valueEntry = entrySupplier.get();
        keyEntry = new StringEntry();
        keyEntry.parse(in);

        return valueEntry.parse(in);
    }
}
