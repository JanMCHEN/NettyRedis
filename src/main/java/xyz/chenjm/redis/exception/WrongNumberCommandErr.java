package xyz.chenjm.redis.exception;

public class WrongNumberCommandErr extends RedisException{
    public WrongNumberCommandErr(String cmd) {
        super("ERR wrong number of arguments for '"+cmd+"' command");
    }
}
