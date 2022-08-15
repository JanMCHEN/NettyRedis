package core.exception;

import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

public class RedisException extends RuntimeException {
    public RedisMessage redisMessage;
    public RedisException(){}
    public RedisException(String message) {
        redisMessage = new ErrorRedisMessage(message);
    }
    public RedisException(RedisMessage message) {
        redisMessage = message;
    }

}
