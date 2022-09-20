package xyz.chenjm.redis.exception;

import xyz.chenjm.redis.core.RedisMessageFactory;

public class ErrorIntException extends RedisMessageException {
    public ErrorIntException() {
        super(RedisMessageFactory.ERR_INT);
    }
}
