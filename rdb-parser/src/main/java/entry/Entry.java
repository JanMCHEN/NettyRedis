package entry;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface Entry {
    String MAGIC = "REDIS";
    int VERSION_LENGTH = 4;

    int EOF = 0xFF;
    int EXPIRE_SECOND_OP = 0xFD;
    int DB_SELECT_OP = 0xFE;
    int EXPIRE_MILL_OP = 0xFC;
    int HASH_SIZE_OP = 0xFB;
    int META_DATA_OP = 0xFA;
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
