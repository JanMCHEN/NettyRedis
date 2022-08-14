package core.structure;

import java.io.Serializable;

public interface RedisObject extends Serializable {
    static int getType(RedisObject obj) {
        if(obj instanceof RedisString) return 0;
        if(obj instanceof RedisList) return 1;
        if(obj instanceof RedisSet) return 2;
        return 5;
    }
    static RedisString.RedisInt valueOf(long value) {
        return RedisString.RedisInt.valueOf(value);
    }
    static RedisString valueOf(byte[] b) {
        try {
            long v = RedisString.parseLong(b, b.length);
            return valueOf(v);
        } catch (NumberFormatException e) {
            return new RedisString.HashString(b);
        }
    }
}
