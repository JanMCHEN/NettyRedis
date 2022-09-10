package xyz.chenjm.redis.core.structure;

import java.nio.charset.Charset;
import java.util.Arrays;

public class RedisSimpleDynamicString implements CharSequence{
    static int M = 1024 * 1024;
    private byte[] value;
    private int length;
    private RedisSimpleDynamicString(byte[] v) {
        value = v;
        length = v.length;
    }
    public RedisSimpleDynamicString(String s) {
        value = s.getBytes();
        length = value.length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return (char) value[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        byte[] copy = Arrays.copyOfRange(value, start, end);
        return new RedisSimpleDynamicString(copy);
    }

    public long append(byte[] b) {
        if(b.length==0) return length;
        if(length+b.length> value.length) {   // 扩容
            resize(length+b.length);
        }
        System.arraycopy(b, 0, value, length, b.length);
        length += b.length;
        return length;
    }

    public void resize(int min) {
        int size = value.length;
        if(size > M) {
            size += M;
        }
        else{
            size *= 2;
        }
        if(size < min) {
            size = min;
        }
        value = Arrays.copyOf(value, size);
    }

    public int setBit(long offset, int bit) {
        int loc = (int) (offset >> 3);
        if(loc >= length && bit==1) {
            length = loc + 1;
        }
        if(length > value.length){
            resize(length);
        }
        offset = offset - ((long) loc << 3);
        int oldBit = 0;
        if(bit==1) {
            oldBit = value[loc];
            value[loc] |= (byte) 1 << offset;
        }
        else if(bit==0 && loc < length){
            oldBit = value[loc];
            value[loc] &= (byte) ~(1 << offset);
        }
        return loc < length && oldBit != value[loc] ? 1: 0;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(value, length);
    }

    @Override
    public String toString() {
        return new String(value, 0, length, Charset.defaultCharset());
    }
}
