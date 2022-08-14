package core.structure;

import utils.StringUtils;

public class RedisString1 implements CharSequence, RedisObject {
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
        if(contents instanceof String) {
            return ((String) contents).getBytes();
        }
        if (contents instanceof RedisSimpleDynamicString) {
            return ((RedisSimpleDynamicString) contents).getBytes();
        }
        if (contents instanceof RedisIntString) {
            return ((RedisIntString) contents).getBytes();
        }
        return new byte[0];
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

    public int increase(long v) {
        if (contents instanceof String) {

        }
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






}
