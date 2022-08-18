package entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库
 */
public class DBEntry implements Entry {
    private static final Logger log = LoggerFactory.getLogger(DBEntry.class);
    /**
     * 数据库号
     */
    private LengthEntry dbNum;
    /**
     *
     */
    private HashSizeEntry hashSizeEntry;
    private List<KeyValueEntry> entries;

    @Override
    public int parse(InputStream in) throws IOException {
        dbNum = new LengthEntry();
        dbNum.parse(in);

        int op = InputStreamUtils.readWithoutEOF(in);

        if (op == HASH_SIZE_OP) {
            hashSizeEntry = new HashSizeEntry();
            hashSizeEntry.parse(in);
            op = InputStreamUtils.readWithoutEOF(in);
        }
        entries = new ArrayList<>();

        // key-value
        for(;;) {
            if (op == DB_SELECT_OP || op == EOF) {
                // next db or end
                return op;
            }
            KeyValueEntry keyValueEntry = new KeyValueEntry();
            op = keyValueEntry.parse(in);
            if (op == -1) {
                op = InputStreamUtils.readWithoutEOF(in);
            }
        }
    }
}
