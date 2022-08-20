package entry;

import util.InputStreamUtils;
import io.NoCopyCharSequence;

import java.io.IOException;
import java.io.InputStream;

public class DoubleEntry implements Entry{
    private int size;
    private CharSequence value;
    @Override
    public int parse(InputStream in) throws IOException {
        size = InputStreamUtils.readWithoutEOF(in);
        if (size >= 0xfd) return -1;
        byte[] bytes = InputStreamUtils.readNBytes(size, in);
        value = new NoCopyCharSequence(bytes);
        return -1;
    }

    public double getValue() {
        switch (size) {
            case 0xfd:
                return Double.NaN;
            case 0xfe:
                return Double.POSITIVE_INFINITY;
            case 0xff:
                return Double.NEGATIVE_INFINITY;
            default:
                break;
        }
        return Double.parseDouble(value.toString());

    }

    @Override
    public String toString() {
        return String.valueOf(getValue());
    }
}
