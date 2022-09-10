package xyz.chenjm.redis.core.structure;

import java.nio.charset.StandardCharsets;

public class RedisIntString implements CharSequence{
    public static RedisIntString build(CharSequence cs) {
        long v = Long.parseLong(cs, 0, cs.length(), 10);
        return new RedisIntString(v);
    }
    public RedisIntString(long v) {
        value = v;
    }
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

    public long getAndIncrease(long v) {
        long old = value;
        value += v;
        return old;
    }

    public long increaseAndGet(long v) {
        value += v;
        return value;
    }

    public byte[] getBytes() {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
