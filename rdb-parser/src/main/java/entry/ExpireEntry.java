package entry;

import util.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;

public abstract class ExpireEntry implements Entry{
    private long value;
    public void setValue(long v) {
        value = v;
    }

    public long getValue() {
        return value;
    }

    public static ExpireEntry getExpireEntry(int op) {
        if (op == EXPIRE_MILL_OP) return new ExpireMillSecondEntry();
        if (op == EXPIRE_SECOND_OP) return new ExpireSecondEntry();
        return null;
    }

    static class ExpireMillSecondEntry extends ExpireEntry {
        @Override
        public int parse(InputStream in) throws IOException {
            setValue(InputStreamUtils.readLong(in));
            return -2;
        }
    }

    static class ExpireSecondEntry extends ExpireEntry {
        @Override
        public int parse(InputStream in) throws IOException {
            setValue(InputStreamUtils.readInt(in));
            return -2;
        }
    }
}
