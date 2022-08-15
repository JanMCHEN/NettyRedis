package core.structure;

import java.io.Serializable;

public interface RedisObject extends Serializable {
    static int getType(RedisObject obj) {
        if(obj instanceof RedisString) return 0;
        if(obj instanceof RedisList) return 1;
        if(obj instanceof RedisSet) return 2;
        return 5;
    }
    static RedisString valueOf(long value) {
        return RedisString.newString(value);
    }
    static RedisString valueOf(byte[] b) {
        return RedisString.newString(new String(b));
    }
}
