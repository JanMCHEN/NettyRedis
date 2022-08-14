package core.structure;

import java.nio.charset.StandardCharsets;

public class RedisIntString implements CharSequence{
    private long value;
    @Override
    public int length() {
        return Long.toString(value).length();
    }

    @Override
    public char charAt(int index) {
        return Long.toString(value).charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return Long.toString(value).subSequence(start, end);
    }

    public long increase(long v) {
        long old = value;
        value += v;
        return old;
    }

    public byte[] getBytes() {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
