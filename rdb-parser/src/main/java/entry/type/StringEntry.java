package entry.type;

import entry.Entry;
import entry.LengthEntry;
import exception.NotSupportedException;
import util.InputStreamUtils;
import util.NoCopyCharSequence;

import java.io.IOException;
import java.io.InputStream;

public class StringEntry implements Entry {
    private LengthEntry length;
    private CharSequence cs;

    public LengthEntry getLength() {
        return length;
    }

    public void setLength(LengthEntry length) {
        this.length = length;
    }

    public CharSequence getCs() {
        return cs;
    }

    public void setCs(CharSequence cs) {
        this.cs = cs;
    }

    @Override
    public int parse(InputStream in) throws IOException {
        length = new LengthEntry();
        length.parse(in);
        int value = length.getValue(), type = length.getType();
        if (type < 3 && value == 0) return -1;  // empty string
        if (type < 3) {
            cs = new NoCopyCharSequence(InputStreamUtils.readNBytes(value, in));
        }else {
            if (value < 3) {
                value = 1 << value;  // 8, 16, 32
                cs = new NoCopyCharSequence(InputStreamUtils.readNBytes(value, in));
            }else {
                // Compressed Strings
                LZFStringEntry lzf = new LZFStringEntry();
                lzf.parse(in);
                cs = lzf;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        if (length.getType() < 3) {
            return "string(" + cs + ")";
        }
        if(length.getValue() < 3) {
            int sz = length.getValue();
            long value = cs.charAt(0);
            if(sz>=1) {
                value |= (cs.charAt(1) << 8);
            }
            if(sz == 2) {
                value |= ((long) cs.charAt(2) <<16) | ((long) cs.charAt(2) << 24);
            }

            return "int(" + value + ")";
        }
        else {
            return cs.toString();
        }
    }

    static class LZFStringEntry implements CharSequence, Entry {
        private LengthEntry cLen, size;
        byte[] value;
        @Override
        public int parse(InputStream in) throws IOException {
            cLen = new LengthEntry(true);
            cLen.parse(in);
            size = new LengthEntry(true);
            size.parse(in);

            value = InputStreamUtils.readNBytes(cLen.getValue(), in);
            return -1;
        }

        @Override
        public int length() {
            return size.getValue();
        }

        @Override
        public char charAt(int index) {
            throw new NotSupportedException("not supported charAt");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new NotSupportedException("not supported subSequence");
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
