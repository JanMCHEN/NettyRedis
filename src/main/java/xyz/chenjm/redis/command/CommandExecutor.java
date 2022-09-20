package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

public interface CommandExecutor {
    void addCommand(Command cmd);

    void addCommand(CommandRunner runner);

    Command getCommand(String... args);

    Object call(RedisClient client, Command cmd, String... args);
}
