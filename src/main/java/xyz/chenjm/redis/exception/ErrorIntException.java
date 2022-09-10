package xyz.chenjm.redis.exception;

import xyz.chenjm.redis.core.RedisMessageFactory;

public class ErrorIntException extends RedisException{
    public ErrorIntException() {
        super(RedisMessageFactory.ERR_INT);
    }
}
