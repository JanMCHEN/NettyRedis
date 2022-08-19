package entry;

import exception.RDBFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CRC64InputStream;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RDBFileEntry implements Entry{
    private static final Logger log = LoggerFactory.getLogger(RDBFileEntry.class);
    HeaderEntry headerEntry;
    List<MetaDataEntry> metaDataEntries;

    List<DBEntry> dataEntries;

    @Override
    public int parse(InputStream in) throws IOException {
        headerEntry = new HeaderEntry();
        headerEntry.parse(in);
        int op = InputStreamUtils.readWithoutEOF(in);
        metaDataEntries = new ArrayList<>();

        while (op == META_DATA_OP) {
            MetaDataEntry meta = new MetaDataEntry();
            metaDataEntries.add(meta);
            meta.parse(in);
            op = InputStreamUtils.readWithoutEOF(in);
        }

        dataEntries = new ArrayList<>();
        while (op == DB_SELECT_OP) {
            DBEntry dbEntry = new DBEntry();
            op = dbEntry.parse(in);
            dataEntries.add(dbEntry);
        }

        if (op == EOF) {
            // check
            if (in instanceof CRC64InputStream) {
                ((CRC64InputStream) in).setCrc_ok(false);
                long crc64 = ((CRC64InputStream) in).getCrcSum();
                long expected = InputStreamUtils.readLong(in);
                if (expected == 0) {
                    log.warn("rdb saved with no crc-sum");
                }
                else if (crc64 != expected) throw new RDBFileException("crc check wrong");
            }
            else {
                InputStreamUtils.readLong(in);
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "RDBFileEntry{" +
                "headerEntry=" + headerEntry +
                ", metaDataEntries=" + metaDataEntries +
                ", dataEntries=" + dataEntries +
                '}';
    }
}
