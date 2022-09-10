package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

/**
 * 在命令执行前后执行， aop切面
 */
public interface RedisCommandAround {

    default void before(RedisClient client, String ...args) {
    }
    default Object after(Object returnValue, RedisClient client, String ...args) {
        return returnValue;
    }
}
