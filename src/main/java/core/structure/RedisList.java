package core.structure;

import java.util.*;

public class RedisList implements RedisObject {
    private final LinkedList<RedisObject> contents;
    private long length;

    public RedisList() {
        contents = new LinkedList<>();
        length = 0;
    }

    private long checkIndex(long index) {
        if(index<0) {
            index += length;
        }
        if(index<0 || index>=length){
            index = length-1;
        }
        return index;
    }

    public boolean addFirst(long value) {
        RedisObject first = contents.peekFirst();
        if(!(first instanceof IntList)) {
//            first = RedisObject.newList();
            first = new IntList();
            contents.addFirst(first);
        }
//        IntList ptr = (IntList) first;
        length ++;
        if(((IntList) first).add(0, value)) return true;
        // 插入失败，新建intList
//        first = RedisObject.newList();
        first = new IntList();
        contents.addFirst(first);
        return ((IntList) first).add(0, value);
    }
    public boolean addFirst(RedisObject value) {
        if(value instanceof RedisString.RedisInt) {
            return addFirst(((RedisString.RedisInt) value).get());
        }
        contents.addFirst(value);
        length ++;
        return true;
    }
    public boolean addLast(long value) {
        RedisObject last = contents.peekLast();
        if(!(last instanceof IntList)) {
//            last = RedisObject.newList();
            last = new IntList();
            contents.addLast(last);
        }
//        IntList ptr = (IntList) last;
        length ++;
        if(((IntList) last).add(value)) return true;
        // 插入失败，新建intList
//        last = RedisObject.newList();
        last = new IntList();
        contents.addFirst(last);
        return ((IntList) last).add(value);
    }
    public boolean addLast(RedisObject value) {
        if(value instanceof RedisString.RedisInt) {
            return addLast(((RedisString.RedisInt) value).get());
        }
        contents.addLast(value);
        length ++;
        return true;
    }
    public RedisObject popFirst() {
        length --;
         RedisObject first = contents.getFirst();
//         if(first==null || !first.isEncodeIntSet())
         if(!(first instanceof RedisList.IntList)){
             contents.removeFirst();
             return first;
         }
//         IntList ptr = (IntList) first.getPtr();
         long l = ((IntList) first).remove(0);
         if(((IntList) first).size()==0){
             contents.removeFirst();
         }
         return new RedisString.RedisInt(l);
     }
     public RedisObject popLast() {
        length -- ;
         RedisObject last = contents.getLast();
//         if(last==null || !last.isEncodeIntSet())
         if(!(last instanceof RedisList.IntList)){
             contents.removeFirst();
             return last;
         }
//         IntList ptr = (IntList) last.getPtr();
         long l = ((IntList) last).remove();
         if(((IntList) last).size()==0){
             contents.removeFirst();
         }
         return new RedisString.RedisInt(l);
     }
    public RedisObject get(long index) {
        index = checkIndex(index);
        RedisObject ans=null;
        if(index==0) {
            ans = contents.getFirst();
            if(ans instanceof IntList) {
                ans = new RedisString.RedisInt(((IntList)ans).get(0));
            }
        }
        else if(index==length-1) {
            ans = contents.getLast();
            if(ans instanceof IntList) {
                ans = new RedisString.RedisInt(((IntList)ans).get());
            }
        }
        else{
            for(RedisObject ele:contents) {
                if(ele instanceof RedisList.IntList){
                    if(index < ((IntList) ele).size()) {
                        ans = new RedisString.RedisInt(((IntList)ele).get((int) index));
                    }
                    else {
                        index -= ((IntList) ele).size();
                    }
                }
                else if(index--==0) {
                    ans = ele;
                }
            }
        }
        return ans;

    }
    public Object[] getRange(long start, long stop) {
        start = checkIndex(start);
        stop = checkIndex(stop)+1;
        if(start >= stop) {
            return new RedisObject[0];
        }
        List<RedisObject> res = new ArrayList<>();
        RedisObject ans;
        ListIterator<RedisObject> iterator = contents.listIterator();
        int cur = 0;
        while (iterator.hasNext()) {
            if(cur>=stop) break;
            ans = iterator.next();
            if(ans instanceof IntList) {
                IntList list = (IntList) ans;
                if(((IntList) ans).length+cur<start){
                    cur += list.length;
                    continue;
                }
                for(int i=0;i<list.length;++i) {
                    if(cur>=stop) break;
                    if(cur++<start) continue;
//                    res.add(new RedisObject(list.get(i)));
                    res.add(new RedisString.RedisInt(list.get(i)));
                }
            }
            else {
                if(cur++ >= start)
                    res.add(ans);
            }
        }

        return res.toArray();

    }

    public long size() {
        return length;
    }
    public boolean isEmpty() {
        return length == 0;
    }

    public static class IntList extends IntSequence{
        public boolean add(long value) {
            return add(length, value);
        }
    }
}
