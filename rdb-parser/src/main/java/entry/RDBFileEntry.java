package entry;

import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RDBFileEntry implements Entry{
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
            // end
        }

        else {
            System.out.println(Integer.toHexString(op));
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
