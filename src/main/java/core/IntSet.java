package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntSet implements RedisSet{
    // IntSet的3种编码类型，分别占用多少个byte
    private static final int INTSET_ENC_INT16 = 2;  // char
    private static final int INTSET_ENC_INT32 = 4;  // int
    private static final int INTSET_ENC_INT64 = 8;  // long

    private static final int INT16_MAX = 0x8fff;
    private static final int INT16_MIN = -0x8fff-1;

    private static final int MAX_LENGTH = 512;



    private int encoding;
    private int length;
    private byte []contents;

    public IntSet() {
        encoding = INTSET_ENC_INT16;
        contents = new byte[0];
    }

    private IntSet(int encoding, int length) {
        this.encoding = encoding;
        this.length = length;
        contents = new byte[length*encoding];
    }

    public static int valueEncoding(long value) {
        if(value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return INTSET_ENC_INT64;
        }
        if (value < INT16_MIN || value > INT16_MAX)
            return INTSET_ENC_INT32;
        return INTSET_ENC_INT16;
    }

    public static long get(byte[] contents, int encoding, int pos) {
        int start = pos * encoding;
        long ans = 0;
        switch (encoding) {
            case INTSET_ENC_INT16:
                ans = contents[start] << 8 | (contents[start+1]&0xff);
                break;
            case INTSET_ENC_INT32:
                ans = contents[start] << 24 | (contents[start+1]&0xff) << 16 | (contents[start+2]&0xff) << 8 | (contents[start+3]&0xff);
                break;
            case INTSET_ENC_INT64:
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

    public long get(int pos) {
        return get(contents, encoding, pos);
    }

    public void set(int pos, long value) {
        int start = pos * encoding;
        for(int i=encoding-1;i>=0;--i) {
            contents[i+start] = (byte) (value & 0xff);
            value >>= 8;
        }
    }

    public int insertPos(long value) {
        int left = 0, right = length,mid = -1;
        long cur;
        while(left < right) {
            mid = (left+right) >> 1;
            if((cur=get(mid))==value) break;
            if(cur > value) right = mid;
            else left = ++mid;
        }
        return mid;
    }

    public boolean search(long value) {
        int pos = insertPos(value);
        return pos > -1 && pos < length && get(pos) == value;
    }

    public boolean add(long value) {
        int enc = valueEncoding(value);
        if(enc > encoding) {
            // 编码升级
            byte[] contents_tmp = contents;
            int encoding_tmp = encoding;
            encoding = enc;
            long v = 0;
            contents = new byte[(length+1)*encoding];
            for(int i=0;i<length;++i) {
                v = get(contents_tmp, encoding_tmp, i);
                set(i, v);
            }
            set(length++, value);
            return true;
        }
        int pos = insertPos(value);
        if(pos==-1) {
            // 数组为空，直接新建一个数组插入
            length ++;
            contents = new byte[encoding];
            set(0, value);
            return true;
        }
        if(pos < length && get(pos)==value) {
            // 已经存在，不插入
            return false;
        }
        contents = Arrays.copyOf(contents, (++length * encoding));
        if (pos==length-1) {
            // 插在尾部
            set(pos, value);
            return true;
        }
        int cur = pos * encoding;
        System.arraycopy(contents, cur, contents, cur + encoding, contents.length - encoding - cur);
        set(pos, value);
        return true;
    }

    public boolean remove(long value) {
        int pos = insertPos(value);
        if(pos > -1 && pos < length && get(pos) == value) {
            System.arraycopy(contents, 0, contents, encoding, pos*encoding);
            length--;
            byte[] contents_ = new byte[length*encoding];
            System.arraycopy(contents, encoding, contents_, 0, contents_.length);
            contents = contents_;
            return true;
        }
        return false;
    }

    public long[] toArray() {
        long []values = new long[length];
        for(int i=0;i<length;++i) {
            values[i] = get(i);
        }
        return values;
    }

    public RedisSet upToHash() {
        RedisSet set = new RedisSet.Hash();
        for(int i=0;i<length;++i) {
            set.add(new RedisObject(get(i)));
        }
        return set;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    public static void main(String[] args) {
        IntSet a = new IntSet();
        a.add(10);
        a.add(0);
        a.add(-1);
        a.add(-2);
        a.add(1L<<33);
        a.remove(0);
        a.add(65536);
        a.add(-65536);
        a.add(-65537);
        a.add(10);
        a.remove(0);
        a.remove(10);
        System.out.println(a);

    }

    @Override
    public long add(RedisObject... values) {
        int ans = 0;
        if(values.length+length>MAX_LENGTH) {
            // 提前失败,不保证实际插入的数据是否会溢出
            return -1;
        }
        for (RedisObject value : values) {
            if (add((Long) value.getPtr())) ans += 1;
        }
        return ans;
    }

    @Override
    public long remove(RedisObject... values) {
        int ans = 0;
        for(var value:values) {
            if(remove((Long) value.getPtr())) ans++;
        }
        return ans;
    }

    @Override
    public List<RedisObject> members() {
        long[] array = toArray();
        List<RedisObject> res = new ArrayList<>(length);
        for(int i=0;i<length;++i) {
            res.add(new RedisObject(array[i]));
        }
        return res;
    }

    @Override
    public boolean contains(RedisObject value) {
        if(!value.isEncodeInt()) return false;
        return search((Long) value.getPtr());
    }

    @Override
    public long size() {
        return length;
    }
}
