package xyz.chenjm.redis.client;

public interface RedisConnection {
    Object sendCommand(Object... args) throws InterruptedException;
}
