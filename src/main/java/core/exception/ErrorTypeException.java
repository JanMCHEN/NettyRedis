package core.exception;

import core.RedisMessageFactory;

public class ErrorTypeException extends RedisException{
    public ErrorTypeException() {
        super(RedisMessageFactory.ERR_TYPE);
    }
}
