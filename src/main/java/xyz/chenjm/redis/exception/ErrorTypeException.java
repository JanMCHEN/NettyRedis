package xyz.chenjm.redis.exception;

import xyz.chenjm.redis.core.RedisMessageFactory;

public class ErrorTypeException extends RedisMessageException {
    public ErrorTypeException() {
        super(RedisMessageFactory.ERR_TYPE);
    }
}
