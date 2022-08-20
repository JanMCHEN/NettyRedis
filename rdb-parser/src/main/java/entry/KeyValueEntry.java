package entry;

import entry.type.*;
import exception.RDBFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 读取数据库键值
 */
public class KeyValueEntry implements Entry{
    private static final Logger log = LoggerFactory.getLogger(KeyValueEntry.class);
    private final static Map<Integer, Supplier<Entry>> typeEntries = new HashMap<>(16, 1.f);
    static {
        typeEntries.put(STRING_TYPE, StringEntry::new);
        typeEntries.put(LIST_TYPE, ListEntry::new);
        typeEntries.put(SET_TYPE, SetEntry::new);
        typeEntries.put(ZSET_TYPE, ZsetEntry::new);
        typeEntries.put(HASH_TYPE, HashEntry::new);
        typeEntries.put(ZIP_MAP_TYPE, ZipMapEntry::new);
        typeEntries.put(ZIP_LIST_TYPE, ZipListEntry::new);
        typeEntries.put(INT_SET_TYPE, IntSetEntry::new);
        typeEntries.put(ZSET_WITH_ZIP_LIST, ZsetWithZipListEntry::new);
        typeEntries.put(HASH_WITH_ZIP_LIST, HashWithZipList::new);
        typeEntries.put(LIST_WITH_QUICK_LIST, QuickListEntry::new);
//        typeEntries.put()
    }
    private ExpireEntry expireEntry;
    private Entry valueEntry;
    private StringEntry keyEntry;

    /**
     * it's a bad idea, this entry I can't deal with better
     */
    public int initOp = -1;

    public void setInitOp(int op) {
        initOp = op;
    }

    @Override
    public int parse(InputStream in) throws IOException {
        int op = initOp > -1 ? initOp: InputStreamUtils.readWithoutEOF(in);
        expireEntry = ExpireEntry.getExpireEntry(op);
        if (expireEntry != null) {
            expireEntry.parse(in);
            op = InputStreamUtils.readWithoutEOF(in);
        }

        if (expireEntry==null && (op==EOF)||(op== SELECT_DB)) {
            log.warn("empty key-value-entry");
            return op;
        }

        Supplier<Entry> entrySupplier = typeEntries.get(op);
        if(entrySupplier==null) {
            throw new RDBFileException("cant parse type: 0x"+ Integer.toHexString(op));
        }
        valueEntry = entrySupplier.get();
        keyEntry = new StringEntry();
        keyEntry.parse(in);

        return valueEntry.parse(in);
    }

    @Override
    public String toString() {
        return "KeyValueEntry{" +
                "expireEntry=" + expireEntry +
                ", keyEntry=" + keyEntry +
                ", valueEntry=" + valueEntry +
                '}';
    }
}
