package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

public interface CommandExecutor {
    void addCommand(RedisCommand cmd);

    void addCommand(CommandRunner runner);

    RedisCommand getCommand(String... args);

    Object call(RedisClient client, RedisCommand cmd, String... args);
}
