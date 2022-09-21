package xyz.chenjm.redis.exception;

import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

public class RedisMessageException extends RedisException {
    public RedisMessage redisMessage;
    public RedisMessageException(String message) {
        super(message);
        redisMessage = new ErrorRedisMessage(message);
    }
    public RedisMessageException(RedisMessage message) {
        super("");
        redisMessage = message;
    }
}
