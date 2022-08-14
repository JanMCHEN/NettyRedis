package core;

public interface CommandFactory {
    RedisCommand getCommand(String key);
}
