package xyz.chenjm.redis.exception;

public class NoSuchCommandErr extends RedisException {
    public NoSuchCommandErr(String cmd) {
        super("ERR unknown command '"+cmd+"'");
    }
}
