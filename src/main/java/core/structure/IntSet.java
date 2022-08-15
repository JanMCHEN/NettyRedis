package core.structure;

import java.util.ArrayList;
import java.util.List;

public class IntSet extends IntSequence implements RedisSet{

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

    @Override
    public boolean add(long value) {
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

        return add(pos, value);
    }

    public boolean delete(long value) {
        int pos = insertPos(value);
        if(pos > -1 && pos < length && get(pos) == value) {
            remove(pos);
            return true;
        }
        return false;
    }

    public RedisSet upToHash() {
        RedisSet set = new RedisSet.Hash();
        for(int i=0;i<length;++i) {
            set.add(RedisString.newString(get(i)));
        }
        return set;
    }

    @Override
    public long add(RedisObject... values) {
        int ans = 0;
        if(values.length+length>MAX_LENGTH) {
            // 提前失败,不保证实际插入的数据是否会溢出
            return -1;
        }
//        for (RedisObject value : values) {
//            if (add(((RedisString)value).get())) ans += 1;
//        }
        return ans;
    }

    @Override
    public long remove(RedisObject... values) {
        int ans = 0;
//        for(RedisObject value:values) {
//            if(delete(((RedisString)value).get())) ans++;
//        }
        return ans;
    }

    @Override
    public List<RedisObject> members() {
        long[] array = toArray();
        List<RedisObject> res = new ArrayList<>(length);
        for(int i=0;i<length;++i) {
            res.add(RedisString.newString(array[i]));
        }
        return res;
    }

    @Override
    public boolean contains(RedisObject value) {
//        if(!value.isEncodeInt()) return false;
//        return search((Long) value.getPtr());
//        if(value instanceof RedisString.RedisInt) {
//            return search(((RedisString.RedisInt) value).get());
//        }
        return false;
    }

}
