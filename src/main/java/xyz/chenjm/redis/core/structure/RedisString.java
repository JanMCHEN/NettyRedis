package xyz.chenjm.redis.core.structure;

import xyz.chenjm.redis.utils.StringUtils;

public class RedisString implements CharSequence, RedisObject {
    public static RedisString newString(CharSequence s) {
        RedisString string = new RedisString();
        string.contents = s;
        try {
            string.convertToIntString();
        }catch (NumberFormatException ignored) {

        }
        return string;
    }
    public static RedisString newString(long v) {
        RedisString string = new RedisString();
        string.contents = new RedisIntString(v);
        return string;
    }
    CharSequence contents;

    @Override
    public int length() {
        return contents.length();
    }

    @Override
    public char charAt(int index) {
        return contents.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return contents.subSequence(start, end);
    }

    public byte[] getBytes() {
        return StringUtils.getBytes(contents);
    }

    public String toString() {
        return contents.toString();
    }

    public int getBit(long offset) {
        return StringUtils.getBit(getBytes(), length(), offset);
    }

    public int setBit(long offset, int bit) {
        return convertToSDS().setBit(offset, bit);
    }

    public long increase(long v) {
        return convertToIntString().increaseAndGet(v);
    }

    public long append(byte[] b) {
        return convertToSDS().append(b);
    }

    public long bitCount() {
        return StringUtils.bitCount(contents);
    }

    private RedisSimpleDynamicString convertToSDS() {
        RedisSimpleDynamicString sds;
        if (contents instanceof RedisSimpleDynamicString) {
            sds = (RedisSimpleDynamicString) contents;
        }
        else {
            sds = new RedisSimpleDynamicString(contents.toString());
            contents = sds;
        }
        return sds;
    }

    private RedisIntString convertToIntString() {
        RedisIntString ins;
        if (contents instanceof RedisIntString) {
            ins = (RedisIntString) contents;
        }
        else {
            ins = RedisIntString.build(contents);
        }
        return ins;
    }
}
