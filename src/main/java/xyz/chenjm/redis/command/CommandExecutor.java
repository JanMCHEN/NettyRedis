package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

public interface CommandExecutor {
    void addCommand(Command2 cmd);

    void addCommand(CommandRunner runner);

    Command2 getCommand(String... args);

    Object call(RedisClient client, Command2 cmd, String... args);
}
