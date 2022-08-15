package core.exception;

import core.RedisMessageFactory;

public class ErrorIntException extends RedisException{
    public ErrorIntException() {
        super(RedisMessageFactory.ERR_INT);
    }
}
