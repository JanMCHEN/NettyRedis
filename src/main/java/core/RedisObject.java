package core;

import java.io.Serializable;
import java.util.WeakHashMap;

public class RedisObject implements Serializable {
    private final static WeakHashMap<String, RedisObject> CACHE = new WeakHashMap<>();
    public static RedisObject valueOf(String value) {
        RedisObject ans = null;
        if((ans= CACHE.get(value))==null) {
            ans = new RedisObject(value);
            CACHE.put(value, ans);
        }
        return ans;
    }

    public static RedisObject createSet() {
        IntSet intSet = new IntSet();
        return createSet(intSet, REDIS_ENCODING_INTSET);
    }

    public static RedisObject createSet(Object obj, int encoding) {
        RedisObject object = new RedisObject(obj);
        object.setType(OBJ_SET);
        object.setEncoding(encoding);
        return object;
    }

    public final static int OBJ_STRING = 0;
    public final static int OBJ_LIST   = 1;
    public final static int OBJ_SET    = 2;
    public final static int OBJ_ZSET   = 3;
    public final static int OBJ_HASH   = 4;

    public final static int OBJ_ENCODING_INT            =  0;       // Long
    public final static int OBJ_ENCODING_EMBSTR         =  1;       // String
    public final static int OBJ_ENCODING_RAW            =  2;       // StringBuilder
    public final static int REDIS_ENCODING_INTSET       =  3;       // IntSet
    public final static int REDIS_ENCODING_HT           =  4;       // HashMap

    private int typeEncoding;     // type and encoding,实际只需用到8位就能存储，高4位存encoding，低4位存type
//    private long lru;
//    private int refCount;
    private final Object ptr;

    public static RedisObject ERROR_TYPE = new RedisObject();

    public RedisObject() {
        this(null);
    }
    public RedisObject(Object ptr) {
        this.ptr = ptr;
    }

    public RedisObject(int type, Object ptr) {
        setType(type);
        if(ptr instanceof String) {
            this.ptr = tryToLong((String) ptr);
        }
        else {
            this.ptr = ptr;
        }
    }

    public RedisObject(String ptr) {
        this(0, ptr);

    }

    public RedisObject(long ptr) {
        this.ptr = ptr;
    }

    public int getEncoding() {
        return typeEncoding >> 4;
    }
    public void setEncoding(int encoding) {
        typeEncoding = typeEncoding & 15 | (encoding << 4);
    }

    public int getType() {
        return typeEncoding & 15;
    }
    public void setType(int type) {
        typeEncoding = typeEncoding & 0xf0 | type;
    }

    public Object getPtr() {
        return ptr;
    }

    public boolean isString() {
        return (typeEncoding & 15) == OBJ_STRING;
    }

    public boolean isEncodeInt() {
        return getEncoding() == OBJ_ENCODING_INT;
    }

    public Object tryToLong(String v) {
        Object value = null;
        try {
            value = Long.parseLong(v);
            setEncoding(OBJ_ENCODING_INT);
        } catch (NumberFormatException e) {
            value = v;
            setEncoding(OBJ_ENCODING_RAW);
        }finally {
            setType(OBJ_STRING);
        }
        return value;
    }

    @Override
    public int hashCode() {
        return ptr.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RedisObject) {
            return ptr.equals(((RedisObject) obj).ptr);
        }
        return false;
    }

    @Override
    public String toString() {
        return ptr.toString();
    }

    public static void main(String[] args) {
        var robj1 = new RedisObject("123");
        System.out.println(robj1.getEncoding());

//        var robj2 = new RedisObject("123");
        System.out.println(robj1);
    }
}
