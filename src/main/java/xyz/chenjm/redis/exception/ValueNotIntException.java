package xyz.chenjm.redis.exception;

public class ValueNotIntException extends RedisException{
    public ValueNotIntException() {
        super("ERR value is not an integer or out of range");
    }
}
