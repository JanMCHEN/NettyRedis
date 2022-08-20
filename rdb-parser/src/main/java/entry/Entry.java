package entry;

import java.io.IOException;
import java.io.InputStream;

public interface Entry {
    String MAGIC = "REDIS";
    int VERSION_LENGTH = 4;

    int EOF = 0xFF;
    int EXPIRE_TIME = 0xFD;
    int EXPIRE_TIME_MS = 0xFC;

    int SELECT_DB = 0xFE;
    int RESIZE_DB = 0xFB;
    int AUX = 0xFA;

    int STRING_TYPE = 0;
    int LIST_TYPE = 1;
    int SET_TYPE = 2;
    int ZSET_TYPE = 3;
    int HASH_TYPE = 4;
    int ZIP_MAP_TYPE = 9;
    int ZIP_LIST_TYPE = 10;
    int INT_SET_TYPE = 11;
    int ZSET_WITH_ZIP_LIST = 12;
    int HASH_WITH_ZIP_LIST = 13;
    int LIST_WITH_QUICK_LIST = 14;


    int parse(InputStream in) throws IOException;
}
