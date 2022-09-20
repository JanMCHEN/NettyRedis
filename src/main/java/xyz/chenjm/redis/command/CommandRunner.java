package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

@FunctionalInterface
public interface CommandRunner {
    Object invoke(RedisClient client, String... args);
}
