package xyz.chenjm.redis.exception;

public class RedisException extends RuntimeException{
    public RedisException(String message) {
        super(message);
    }
}
