package core;

import io.netty.handler.codec.redis.*;

public class RedisMessagePool {
    public static final RedisMessage OK = new SimpleStringRedisMessage("OK");
    public static final RedisMessage QUEUED = new SimpleStringRedisMessage("QUEUED");
    public static final RedisMessage NULL = FullBulkStringRedisMessage.NULL_INSTANCE;
    public static final RedisMessage EMPTY = ArrayRedisMessage.EMPTY_INSTANCE;
    public static final RedisMessage ERR = new ErrorRedisMessage("ERR");
    public static final RedisMessage ERR_SYNTAX = new ErrorRedisMessage("ERR syntax error");
    public static final RedisMessage ERR_INT = new ErrorRedisMessage("ERR value is not an integer or out of range");
    public static final RedisMessage ERR_EXEC_ERR = new ErrorRedisMessage(" EXECABORT Transaction discarded because of previous errors.");
    public static final RedisMessage ERR_EXEC_MUL = new ErrorRedisMessage("ERR EXEC without MULTI");
    public static final RedisMessage ERR_MULTI = new ErrorRedisMessage("ERR MULTI calls can not be nested");
    public static final RedisMessage ERR_WATCH = new ErrorRedisMessage("ERR WATCH inside MULTI is not allowed");
    public static final RedisMessage ERR_DISCARD = new ErrorRedisMessage("ERR DISCARD without MULTI");
    public static final RedisMessage ERR_TYPE = new ErrorRedisMessage("WRONGTYPE Operation against a key holding the wrong kind of value");
    public static final RedisMessage ERR_SEL = new ErrorRedisMessage("ERR invalid DB index");

    public static final RedisMessage BG_SAVE = new SimpleStringRedisMessage("Background saving started");
}
