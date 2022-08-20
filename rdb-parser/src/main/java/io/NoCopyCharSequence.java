package io;

/**
 * 减少byte的复制
 */
public class NoCopyCharSequence implements CharSequence {
    byte[] value;

    public NoCopyCharSequence(byte[] v) {
        value = v;
    }

    @Override
    public int length() {
        return value.length;
    }

    @Override
    public char charAt(int index) {
        return (char) (value[index] & 0xff);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new String(value, start, end);
    }

    @Override
    public String toString() {
        return new String(value);
    }

    public byte[] getBytes() {
        return value;
    }
}
