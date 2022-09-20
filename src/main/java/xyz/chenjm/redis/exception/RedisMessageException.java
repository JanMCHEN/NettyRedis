package xyz.chenjm.redis.exception;

import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

public class RedisMessageException extends RuntimeException {
    public RedisMessage redisMessage;
    public RedisMessageException(){}
    public RedisMessageException(String message) {
        redisMessage = new ErrorRedisMessage(message);
    }
    public RedisMessageException(RedisMessage message) {
        redisMessage = message;
    }
}
