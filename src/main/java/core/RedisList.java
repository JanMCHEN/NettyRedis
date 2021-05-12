package core;

import java.util.*;

public class RedisList {
    private final LinkedList<RedisObject> contents;
    private long length;

    public RedisList() {
        contents = new LinkedList<>();
        length = 0;
    }

    static class Node {
        RedisObject e;
        int index;
        Node(RedisObject e, int index) {
            this.e = e;
            this.index = index;
        }
         static Node newNode(RedisObject e, int index) {
            return new Node(e, index);
         }
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
        if(first==null || !first.isEncodeIntSet()) {
            first = RedisObject.newList();
            contents.addFirst(first);
        }
        IntList ptr = (IntList) first.getPtr();
        length ++;
        if(ptr.add(0, value)) return true;
        // 插入失败，新建intList
        first = RedisObject.newList();
        contents.addFirst(first);
        return ((IntList) first.getPtr()).add(0, value);
    }
    public boolean addFirst(RedisObject value) {
        if(value.getEncoding()==RedisObject.OBJ_ENCODING_INT) {
            return addFirst((Long)value.getPtr());
        }
        contents.addFirst(value);
        length ++;
        return true;
    }
    public boolean addLast(long value) {
        RedisObject last = contents.peekFirst();
        if(last==null || !last.isEncodeIntSet()) {
            last = RedisObject.newList();
            contents.addLast(last);
        }
        IntList ptr = (IntList) last.getPtr();
        length ++;
        if(ptr.add(value)) return true;
        // 插入失败，新建intList
        last = RedisObject.newList();
        contents.addLast(last);
        boolean ans = ((IntList) last.getPtr()).add(value);
        return ans;
    }
    public boolean addLast(RedisObject value) {
        if(value.getEncoding()==RedisObject.OBJ_ENCODING_INT) {
            return addLast((Long)value.getPtr());
        }
        contents.addLast(value);
        length ++;
        return true;
    }
    public RedisObject popFirst() {
        length --;
         RedisObject first = contents.getFirst();
         if(first==null || !first.isEncodeIntSet()){
             contents.removeFirst();
             return first;
         }
         IntList ptr = (IntList) first.getPtr();
         long l = ptr.remove(0);
         if(ptr.size()==0){
             contents.removeFirst();
         }
         return new RedisObject(l);
     }
     public RedisObject popLast() {
        length -- ;
         RedisObject last = contents.getLast();
         if(last==null || !last.isEncodeIntSet()){
             contents.removeLast();
             return last;
         }
         IntList ptr = (IntList) last.getPtr();
         long l = ptr.remove();
         if(ptr.size()==0){
             contents.removeLast();
         }
         return new RedisObject(l);
     }
    public RedisObject get(long index) {
        index = checkIndex(index);
        RedisObject ans=null;
        if(index==0) {
            ans = contents.getFirst();
            if(ans != null && ans.isEncodeIntSet()) {
                ans = new RedisObject(((IntList)ans.getPtr()).get(0));
            }
        }
        else if(index==length-1) {
            ans = contents.getLast();
            if(ans != null && ans.isEncodeIntSet()) {
                ans = new RedisObject(((IntList)ans.getPtr()).get());
            }
        }
        else{
            for(RedisObject ele:contents) {
                if(ele.isEncodeIntSet()){
                    IntList ptr = (IntList) ele.getPtr();
                    if(index < ptr.size()) {
                        ans = new RedisObject(((IntList)ele.getPtr()).get((int) index));
                    }
                    else {
                        index -= ptr.size();
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
        if(start>= stop) {
            return new RedisObject[0];
        }
        List<RedisObject> res = new ArrayList<>();
        RedisObject ans;
        ListIterator<RedisObject> iterator = contents.listIterator();
        int cur = 0;
        while (iterator.hasNext()) {
            if(cur>=stop) break;
            ans = iterator.next();
            if(ans.isEncodeIntSet()) {
                IntList list = (IntList) ans.getPtr();
                if(list.length+cur<start){
                    cur += list.length;
                    continue;
                }
                for(int i=0;i<list.length;++i) {
                    if(cur++>=stop) break;
                    res.add(new RedisObject(list.get(i)));
                }
            }
            else {
                res.add(ans);
                cur++;
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
