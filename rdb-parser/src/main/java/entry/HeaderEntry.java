package entry;

import util.InputStreamUtils;
import exception.RDBFileException;
import util.NoCopyCharSequence;

import java.io.IOException;
import java.io.InputStream;

public class HeaderEntry implements Entry{
    static final String MAGIC = "REDIS";
    static final int VERSION_LENGTH = 4;
    CharSequence magic, version;

    @Override
    public int parse(InputStream in) throws IOException {
        magic = new String(InputStreamUtils.readNBytes(MAGIC.length(), in));
        if(!MAGIC.contentEquals(magic)) {
            throw new RDBFileException("magic wrong");
        }
        version = new NoCopyCharSequence(InputStreamUtils.readNBytes(VERSION_LENGTH, in));
        return -1;
    }

    @Override
    public String toString() {
        return "HeaderEntry{" +
                "magic='" + magic + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
