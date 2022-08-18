import entry.MetaDataEntry;
import entry.RDBFileEntry;
import exception.RDBFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class RDBFile implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(RDBFile.class);

    private InputStream rdb;
    private RDBFileEntry entry;

    public RDBFile(InputStream in) {
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
