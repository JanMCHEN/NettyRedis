import entry.RDBFileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputStreamUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RDBFile implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(RDBFile.class);

    private InputStream rdb;
    private RDBFileEntry entry;

    private boolean crc64Check = true;

    public RDBFile(InputStream in) {
        if (crc64Check) {
            in = InputStreamUtils.CRC64InputStreamWrapper(in);
        }
        rdb = in;
        entry = new RDBFileEntry();
    }

    public void decode() throws IOException {
        entry.parse(rdb);
    }

    @Override
    public void close() throws Exception {
        if(rdb!=null) {
            rdb.close();
        }
    }

    public static void main(String[] args) throws IOException {
        FileInputStream file = new FileInputStream("/home/cjm/dump.rdb");
        RDBFile rdbFile = new RDBFile(file);
        rdbFile.decode();
        System.out.println(rdbFile.entry);
    }
}
