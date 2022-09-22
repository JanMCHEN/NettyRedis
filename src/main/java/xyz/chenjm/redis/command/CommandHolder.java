package xyz.chenjm.redis.command;

public interface CommandHolder {
    void addCommand(RedisCommand cmd);

    void addCommand(CommandRunner runner);

    RedisCommand getCommand(String... args);
}
