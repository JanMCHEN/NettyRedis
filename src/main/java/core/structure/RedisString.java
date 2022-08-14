package core.structure;

import java.util.Arrays;

public interface RedisString extends RedisObject {
    private static byte[] toArray(long v) {
        int size = size(v);
        byte[] b = new byte[size];
        if(v<0) {
            b[0] = '-';
            for(int i=size-1;i>0;--i) {
                b[i] = (byte) ('0' - v % 10);
                v /= 10;
            }
        }
        else {
            for (int i = size - 1; i >= 0; --i) {
                b[i] = (byte) ('0' + v % 10);
                v /= 10;
            }
        }
        return b;
    }
    private static int size(long v) {
        if(v == Long.MIN_VALUE) return 20;
        int ans = 0;
        if(v<=0) {
            v = -v;
            ans++;
        }
        while(v>0) {
            ans ++;
            v /= 10;
        }
        return ans;
    }
    private static long bitCount(byte[] b, int length) {
        long ans = 0;
        int cur;
        for(int i=0;i<length;++i) {
            cur = b[i] & 0xff;
            while (cur>0){
                cur &= (cur-1);
                ans ++;
            }
        }
        return ans;
    }
    private static int getBit(byte[] b, int length, long offset) {
        int loc = (int) (offset >> 3);
        if(loc >= length){
            return 0;
        }
        offset = offset - ((long) loc << 3);
        return ((b[loc] & 0xff) >> offset) & 1;
    }
    static long parseLong(byte[] contents, int length) {
        if(length<=0 || length > 20 || (length==20 && contents[0]!='-') || length > contents.length) {
            throw new NumberFormatException();
        }
        boolean neg = contents[0]=='-';
        int i = neg ? 1 : 0;
        long ans = 0;
        int tmp;
        for(;i<length;++i) {
            tmp = contents[i] - '0';
            if(tmp>=0 && tmp<=9) {
                ans = ans * 10 + tmp;
            }
            else {
                throw new NumberFormatException();
            }
        }
        return neg ? -ans: ans;
    }
    static long parseLong(byte[] contents) {
        return parseLong(contents, contents.length);
    }
    static boolean equals(byte[] b, String s) {
        if(b.length != s.length()) return false;
        for(int i=0;i<b.length;++i) {
            if(b[i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    static RedisString toRaw(RedisString obj) {
        return new RawString(obj.getBytes());
    }

    long get();
    byte[] getBytes();
    void setBit(long offset, int bit);
    int getBit(long offset);
    long bitCount();
    long increase(long v);
    int size();

    class RedisInt implements RedisString {
        static RedisInt[] INT_CACHE = new RedisInt[128];
        static {
            for(int i=0;i<INT_CACHE.length;++i) {
                INT_CACHE[i] = new RedisInt(i);
            }
        }
        static RedisInt valueOf(long value) {
            if(value>=0 && value < RedisString.RedisInt.INT_CACHE.length) {
                return RedisString.RedisInt.INT_CACHE[(int)value];
            }
            return new RedisString.RedisInt(value);
        }

        private final long contents;
        public RedisInt(long val) {
            contents = val;
        }

        @Override
        public long get() {
            return contents;
        }

        @Override
        public long increase(long v) {
            throw new RuntimeException("not supported");
        }

        @Override
        public byte[] getBytes() {
            return toArray(contents);
        }

        @Override
        public void setBit(long offset, int bit) {
            throw new RuntimeException("not supported");
        }

        @Override
        public int getBit(long offset) {
            byte[] b = getBytes();
            return RedisString.getBit(b, b.length, offset);
        }

        @Override
        public long bitCount() {
            byte[] b = toArray(contents);
            return RedisString.bitCount(b, b.length);
        }

        @Override
        public int size() {
            return RedisString.size(contents);
        }

        @Override
        public int hashCode() {
            return (int) (contents ^ (contents >>> 32));
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Long){
                return ((Long) obj).intValue() == contents;
            }
            if(obj instanceof RedisInt) {
                return ((RedisInt) obj).contents == contents;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.valueOf(contents);
        }
    }
    class IntString implements RedisString {
        private long contents;
        public IntString(long val) {
            contents = val;
        }

        @Override
        public long get() {
            return contents;
        }

        @Override
        public long increase(long v) {
            contents += v;
            return contents;
        }

        @Override
        public byte[] getBytes() {
            return toArray(contents);
        }

        @Override
        public void setBit(long offset, int bit) {
            throw new RuntimeException("not supported");
        }

        @Override
        public int getBit(long offset) {
            byte[] b = getBytes();
            return RedisString.getBit(b, b.length, offset);
        }

        @Override
        public long bitCount() {
            byte[] b = toArray(contents);
            return RedisString.bitCount(b, b.length);
        }

        @Override
        public int size() {
            return RedisString.size(contents);
        }

        @Override
        public String toString() {
            return String.valueOf(contents);
        }
    }
    class HashString implements RedisString {
        final byte[] contents;

        public HashString(byte[] contents) {
            this.contents = contents;
        }
        public HashString(String s) {
            this.contents = s.getBytes();
        }

        @Override
        public long get() {
            return parseLong(contents, contents.length);
        }

        @Override
        public byte[] getBytes() {
            return contents;
        }

        @Override
        public void setBit(long offset, int bit) {
            throw new RuntimeException("not supported");
        }

        @Override
        public int getBit(long offset) {
            return RedisString.getBit(contents, contents.length, offset);
        }

        @Override
        public long bitCount() {
            return RedisString.bitCount(contents, contents.length);
        }

        @Override
        public long increase(long v) {
            throw new RuntimeException("not supported");
        }

        @Override
        public int size() {
            return contents.length;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(contents);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj==this) return true;
            if(obj instanceof HashString) {
                HashString hObj = (HashString) obj;
                return Arrays.equals(contents, hObj.contents);
            }
            return false;
        }

        @Override
        public String toString() {
            return new String(contents);
        }

    }
    class RawString implements RedisString {
        static int M = 1024 * 1024;
        private int length;
        private byte[] contents;
        public RawString(byte[] b) {
            length = b.length;
            contents = Arrays.copyOf(b, length);
        }
        public RawString(long v) {
            contents = RedisString.toArray(v);
            length = contents.length;
        }
        public RawString(RedisString s) {
            contents = s.getBytes();
            length = contents.length;
        }

        @Override
        public long get() {
            return parseLong(contents, length);
        }

        public long append(byte[] b) {
            if(b.length==0) return length;
            if(length+b.length>contents.length) {   // 扩容
                resize(length+b.length);
            }
            System.arraycopy(b, 0, contents, length, b.length);
            length += b.length;
            return length;
        }

        public void resize(int min) {
            int size = contents.length;
            if(size > M) {
                size += M;
            }
            else{
                size *= 2;
            }
            if(size < min) {
                size = min;
            }
            contents = Arrays.copyOf(contents, size);
        }

        @Override
        public byte[] getBytes() {
            if(length==contents.length) return contents;
            return Arrays.copyOf(contents, length);
        }

        @Override
        public void setBit(long offset, int bit) {
            int loc = (int) (offset >> 3);
            if(loc >= length) {
                length = loc + 1;
            }
            if(loc >= contents.length){
                resize(loc+1);
            }
            offset = offset - (loc << 3);
            if(bit==1) {
                contents[loc] |= (byte) 1 << offset;
            }
            else if(bit==0){
                contents[loc] &= (byte) ~(1 << offset);
            }
        }

        @Override
        public int getBit(long offset) {
            if(offset<0) return 0;
            int loc = (int) (offset >> 3);
            if(loc >= length){
                return 0;
            }
            offset = offset - (loc << 3);
            return ((contents[loc] & 0xff) >> offset) & 1;
        }

        @Override
        public long bitCount() {
            return RedisString.bitCount(contents, length);
        }

        @Override
        public long increase(long v) {
            throw new RuntimeException("not supported");
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj==this) return true;
            if(obj instanceof RawString) {
                return ((RawString) obj).length == length && Arrays.equals(contents, 0, length, ((RawString) obj).contents, 0, length);
            }
            if(obj instanceof String) {
                return RedisString.equals(contents, (String) obj);
            }
            return false;
        }

        @Override
        public String toString() {
            return new String(getBytes());
        }

        public static void main(String[] args) {
            RawString embString = new RawString(-910);
//            embString.setBit(10, 1);
//            embString.setBit(11, 1);
//            embString.setBit(199,1);
            System.out.println(embString.getBit(10));
            System.out.println(embString.bitCount());
            System.out.println(embString.get());
//            IntString intString = new IntString(Long.MIN_VALUE);
//            System.out.println(intString.size());
//            System.out.println(intString.bitCount());
        }
    }




}
