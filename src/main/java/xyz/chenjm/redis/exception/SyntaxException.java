package xyz.chenjm.redis.exception;

public class SyntaxException extends RedisException{
    public SyntaxException() {
        super("ERR syntax error");
    }
}
