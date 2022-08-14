package core.structure;

import java.util.Arrays;

public abstract class IntSequence implements RedisObject {
    protected static final int ENC_INT16 = 2;  // char
    protected static final int ENC_INT32 = 4;  // int
    protected static final int ENC_INT64 = 8;  // long

    protected static final int INT16_MAX = 0x8fff;
    protected static final int INT16_MIN = -0x8fff-1;

    protected static final int MAX_LENGTH = 512;

    protected int encoding;     // 编码方式，char，int，long
    protected int length;       // 实际存放数据的长度，=contents.length/encoding
    protected byte[] contents;  // 数据容器

    public IntSequence() {
        this(ENC_INT16, 0);
    }
    protected IntSequence(int encoding, int length) {
        if(encoding==ENC_INT16 || encoding==ENC_INT32 || encoding == ENC_INT64) {
            this.encoding = encoding;
        }
        else {
            throw new RuntimeException("Not supported encoding:"+encoding);
        }
        if(length < 0) {
            throw new RuntimeException("length must>0,yours +"+length);
        }

        this.length = Math.min(length, MAX_LENGTH);
        contents = new byte[length*encoding];
    }

    public static int valueEncoding(long value) {
        if(value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return ENC_INT64;
        }
        if (value < INT16_MIN || value > INT16_MAX)
            return ENC_INT32;
        return ENC_INT16;
    }

    /**
     * 把byte数组当成encoding类型的数组，过去第i个位置的值，将内部实现抽象，可以当成char[],int[],long[]
     * @param contents 待计算byte数组
     * @param encoding 编码类型
     * @param pos      获取pos位置上的值
     * @return  long
     */
    public static long get(byte[] contents, int encoding, int pos) {
        int start = pos * encoding;
        long ans = 0;
        switch (encoding) {
            case ENC_INT16:
                ans = contents[start] << 8 | (contents[start+1]&0xff);
                break;
            case ENC_INT32:
                ans = contents[start] << 24 | (contents[start+1]&0xff) << 16 | (contents[start+2]&0xff) << 8 | (contents[start+3]&0xff);
                break;
            case ENC_INT64:
                ans = contents[start];
                for(int i=start+1;i<start+encoding;++i) {
                    ans = ans << 8 | (contents[i]&0xff);
                }
                break;
            default:
                break;
        }
        return ans;
    }

    /**
     * 将给定的值赋给指定位置，不考虑value的范围是否超过encoding，应该在具体的实现中考虑
     * @param contents 操作的bytes
     * @param pos   位置
     * @param encoding 编码方式
     * @param value    值
     */
    public static void set(byte[] contents, int pos, int encoding, long value) {
        int start = pos * encoding;
        for(int i=encoding-1;i>=0;--i) {
            contents[i+start] = (byte) (value & 0xff);
            value >>= 8;
        }
    }

    public long get(int pos) {
        return get(contents, encoding, pos);
    }
    public long get() {
        return get(length-1);
    }
    public void set(int pos, long value) {
        set(contents, pos, encoding, value);
    }

    public abstract boolean add(long value);
    public boolean add(int pos, long value) {
        if(length == MAX_LENGTH) {
            return false;
        }
        int enc = valueEncoding(value);
        byte[] newContents;
        if(enc > encoding) {
            // 编码升级
            newContents = new byte[(length+1) * enc];
            for(int i=0;i<length;++i) {
                set(newContents, i, enc, get(i));
            }
            contents = newContents;
            encoding = enc;
        }
        else {
            newContents = Arrays.copyOf(contents, (length+1) * encoding);
        }
        if (pos<length) {
            int cur = pos * encoding;
            System.arraycopy(contents, cur, newContents, cur + encoding, newContents.length - encoding - cur);
        }
        set(newContents, pos, encoding, value);
        contents = newContents;
        length ++;
        return true;
    }

    public long remove(int index) {
        long ans;
        if(index > -1 && index < length) {
            ans = get(index);
            if(index>0) System.arraycopy(contents, 0, contents, encoding, index*encoding);
            byte[] newContents = new byte[(length-1)*encoding];
            System.arraycopy(contents, encoding, newContents, 0, newContents.length);
            length--;
            contents = newContents;
            return ans;
        }
        else
            throw new IndexOutOfBoundsException();
    }
    public long remove() {
        return remove(length-1);
    }

    public long[] toArray() {
        long []values = new long[length];
        for(int i=0;i<length;++i) {
            values[i] = get(i);
        }
        return values;
    }

    public int size() {
        return length;
    }
    public boolean isEmpty() {
        return length == 0;
    }
    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
