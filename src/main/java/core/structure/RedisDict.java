package core.structure;

import java.util.*;

public class RedisDict<K, V> extends AbstractMap<K, V> implements RedisObject{

    static final int DEFAULT_INITIAL_CAPACITY = 4;
    static final float DEFAULT_LOAD_FACTOR = 1.f;    // rehash
    static final float FORCE_LOAD_FACTOR = 5.f;
    static final int MINIMUM_LOAD_TIMES = 10;
    static final int MAXIMUM_CAPACITY = 1 << 30;

    static final Random RANDOM = new Random();

    int threshold;

    static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static Node<?, ?>[] newTable(int cap) {
        cap = cap<=DEFAULT_INITIAL_CAPACITY ? DEFAULT_INITIAL_CAPACITY: tableSizeFor(cap);
        return new Node[cap];
    }

    public static class Node<K,V> implements Entry<K,V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue());
            }
            return false;
        }
    }

    static class Dict<K, V>{
        transient int size;
        transient Node<K,V>[] table;

        Dict() {
        }

        Dict(int cap) {
            table = (Node<K, V>[]) newTable(cap);
        }

        Node<K, V> getNode(int hash, Object key) {
            int n;
            Node<K, V> e;
            if (table != null && (n=table.length)>0 && (e=table[hash & (n-1)])!=null) {
                while (e != null && e.key != key && !e.key.equals(key)) {
                    e = e.next;
                }
                return e;
            }
            return null;
        }

        Node<K, V> getNode(Object key) {
            if (table==null) return null;
            int hash = hash(key);
            return getNode(hash, key);
        }

        Node<K, V> removeNode(Object key) {
            int i, n, hash=hash(key);
            Node<K, V> e, pre;
            if (table != null && (n=table.length)>0 && (e=table[i=hash & (n-1)])!=null) {
                if (e.key == key || e.key.equals(key)) {
                    table[i] = e.next;
                    size--;
                }
                else {
                    pre = e; e = e.next;
                    while (e != null && e.key != key && !e.key.equals(key)) {
                        pre = e;
                        e = e.next;
                    }
                    if (e == null) {
                        return null;
                    }
                    pre.next = e.next;
                    size--;
                    return e;
                }
            }
            return null;
        }

        public boolean containsKey(Object key) {
            return getNode(key) != null;
        }

        public V get(Object key) {
            Node<K, V> e;
            return (e = getNode(key)) == null ? null:e.value;
        }

        public V put(K key, V value) {
            Node<K,V>[] tab; Node<K,V> p; int n, i;
            if ((tab=table)==null || (n=tab.length)==0) {
                tab = (Node<K, V>[]) newTable(0);
                n = tab.length;
                table = tab;
            }
            if ((p=tab[i=hash(key)&(n-1)])==null) {
                tab[i] = new Node<>(key, value, null);
            }
            else {
                Node<K, V> e;
                for (e=p; e!=null && e.key != key && !e.key.equals(key);e=e.next);
                if (e==null) {
                    tab[i] = new Node<>(key, value, p);   // put at first
                }
                else {
                    // put if not absent
                    V oldValue = e.value;
                    e.value = value;
                    return oldValue;
                }
            }
            ++size;
            return null;
        }

        Node<K, V>  put(int hash, Node<K, V> node) {
            int i, sizeMask = table.length - 1;
            Node<K, V> next = node.next;
            node.next = table[i=hash & sizeMask];
            table[i] = node;
            size++;
            return next;
        }

        public V remove(Object key) {
            Node<K, V> node = removeNode(key);
            return node == null ? null: node.value;
        }

        public void clear() {
            size = 0;
            table = null;
        }
    }

    Dict<K, V>[] ht = new Dict[2];
    int rehashIdx = -1;

    public RedisDict() {
        ht[0] = new Dict<>();
        ht[1] = new Dict<>();
        threshold = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }

    void tryResize(boolean up) {
        if (isRehashing() || ht[0].table==null) return;
        int len = ht[0].table.length;
        if (up && ht[0].size >= threshold && len < MAXIMUM_CAPACITY) {
            ht[1].table = (Node<K, V>[]) newTable(len << 1);
            threshold = (int) (ht[1].table.length * DEFAULT_LOAD_FACTOR);
            rehashIdx = 0;
        } else if (!up && ht[0].size * MINIMUM_LOAD_TIMES <= len) {
            ht[1].table = (Node<K, V>[]) newTable(len >> 1);
            threshold = (int) (ht[1].table.length * DEFAULT_LOAD_FACTOR);
            rehashIdx = 0;
        }
    }

    int tryRehash(int n) {
        if (!isRehashing()) {
            return 0;
        }
        int empty_visits = n*10; /* Max number of empty buckets to visit. */
        Node<K, V>[] table = ht[0].table;
        while(n-- > 0 && ht[0].size > 0) {
            Node<K, V> node;
            while ((node=table[rehashIdx])==null) {
                rehashIdx++;
                if (empty_visits-- == 0) return 1;
            }

            while (node != null) {
                node = ht[1].put(hash(node.key), node);
                ht[0].size--;
            }

            table[rehashIdx++] = null;
        }

        // finish rehash
        if (ht[0].size==0) {
            ht[0] = ht[1];
            ht[1] = new Dict<>();
            rehashIdx = -1;
            return 0;
        }

        return 1;

    }

    public boolean isRehashing() {
        return rehashIdx > -1;
    }

    @Override
    public int size() {
        return ht[0].size + ht[1].size;
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public boolean containsKey(Object o) {
        tryRehash(1);
        return size() > 0 && (ht[0].containsKey(o) || (isRehashing() && ht[1].containsKey(o)));
    }

    @Override
    public boolean containsValue(Object o) {
        return false;
    }

    @Override
    public V get(Object o) {
        V v = ht[0].get(o);
        tryRehash(1);
        return v == null && isRehashing() ? ht[1].get(o): v;
    }

    @Override
    public V put(K k, V v) {
        V old;
        if (isRehashing()) {
            Node<K, V> find = ht[0].getNode(k);
            if (find!=null) {
                old = find.value;
                find.value = v;
            }
            else {
                old = ht[1].put(k, v);
            }

        }

        else {
            old = ht[0].put(k, v);
        }

        tryResize(true);

        tryRehash(1);

        return old;
    }

    @Override
    public V remove(Object o) {
        V r = ht[0].remove(o);
        if (r==null && isRehashing()) {
            r = ht[1].remove(o);
        }
        tryResize(false);
        tryRehash(1);
        return r;
    }

    @Override
    public void clear() {
        ht[0].clear();
        ht[1].clear();
        threshold = (int) (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
    }

    public Node<K, V> getRandom() {
        if (isEmpty()) return null;
        int idx;
        Node<K, V> he=null;
        tryRehash(1);

        if(isRehashing()) {
            int len = ht[0].table.length + ht[1].table.length - rehashIdx;
            while(he == null) {
                idx = rehashIdx + RANDOM.nextInt(len);
                he = idx >= ht[0].table.length ? ht[1].table[idx-ht[0].table.length]: ht[0].table[idx];
            }
        }
        else {
            while (he == null) {
                idx = RANDOM.nextInt(ht[0].table.length);
                he = ht[0].table[idx];
            }
        }
        // find the bucket, then find the next;
        // 水塘抽样
        Node<K, V> ori = he;
        he = he.next;
        int len=2;
        while (he != null) {
            if (RANDOM.nextInt(len++)==0) ori = he;
            he = he.next;
        }
        return ori;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return RedisDict.this.iterator();
        }

        @Override
        public int size() {
            return RedisDict.this.size();
        }
    }

    final class DictIterator implements Iterator<Entry<K, V>> {

        Node<K, V> next;  // for hasNext
        Node<K, V> current;  // for remove
        int tableIdx;
        int index;

        DictIterator() {
            current = null;
            tableIdx = 0;
            index = 0;

            setNext();
        }

        void setNext() {
            if (next != null && (next=next.next) != null) {
                return;
            }
            RedisDict<K, V> dict = RedisDict.this;

            Node<K, V>[] table = dict.ht[tableIdx].table;
            if (table == null) return;
            while (index < table.length && (next=table[index++]) == null) {}

            if (next == null) {
                if (isRehashing() && tableIdx==0) {
                    tableIdx++;
                    index = 0;
                    setNext();
                }
            }
        }


        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Entry<K, V> next() {
            Node<K, V> e = next;
            if (e==null) throw new NoSuchElementException();
            setNext();
            current = e;
            return current;
        }

        @Override
        public void remove() {
            if (current != null) {
                RedisDict.this.remove(current.key);
                current = null;
            }

        }
    }

    public Iterator<Entry<K, V>> iterator() {
        return new DictIterator();
    }

}
