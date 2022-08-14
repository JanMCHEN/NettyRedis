package core.exception;

import core.RedisMessagePool;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

public class RedisException extends RuntimeException {
    public static RedisException ERROR_TYPE = new RedisException(RedisMessagePool.ERR_TYPE);
    public static RedisException ERROR_INT = new RedisException(RedisMessagePool.ERR_INT);

    public RedisMessage redisMessage;
    public RedisException(){}
    public RedisException(String message) {
        redisMessage = new ErrorRedisMessage(message);
    }
    public RedisException(RedisMessage message) {
        redisMessage = message;
    }

}
