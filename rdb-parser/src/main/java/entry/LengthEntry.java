package entry;

import exception.RDBFileException;
import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 长度编码
 * type=0: value=0-2^6-1
 * type=1: value=2^6-2^14-1
 * type=2: value=2^14-2^32-1   but in java maxInt=2^31 - 1;
 * type=3: value is the other entry's type
 */
public class LengthEntry implements Entry{
    public LengthEntry() {
    }

    public LengthEntry(boolean isInt) {
        if(isInt) {
            // 指定必须为int类型
            type = -1;
        }
    }
    int value, type;

    @Override
    public int parse(InputStream in) throws IOException {
        int b = InputStreamUtils.readWithoutEOF(in);
        int code = b >> 6;
        if (type < -1 && code==3) {
            throw new RDBFileException("not a integer");
        }
        type = code;
        b = b & 0x3f;
        if (type == 0) {
            value = b;                       // 6 bit
        } else if (type == 1) {
            value = InputStreamUtils.readWithoutEOF(in)<<8 | b ;     // 14 bit
        } else if (type == 2) {
            value = InputStreamUtils.readInt(in);   // 32 bit
        }else {
            value = b;                     // 6 bit
        }
        return -1;
    }

    public int getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "LengthEntry{" +
                "value=" + value +
                '}';
    }
}
