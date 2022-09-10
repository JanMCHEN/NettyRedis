package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisDB;

/**
 * 不同的数据类型都是用Object存储的，当操作某一特定数据类型时需要进行类型判断
 * @param <T>
 */
public interface TypeAdapter<T> {
    /**
     * 返回key对应的值类型
     * @param db 操作的数据库
     * @param key 键值
     * @throws xyz.chenjm.redis.exception.ErrorTypeException if 类型不匹配
     */
    T get(RedisDB db, String key);
}
