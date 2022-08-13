package core;

public interface CommandFactory {
    RedisCommand<?> getCommand(String key);
    <T> RedisCommand<T> getCommand(String key, Class<T> returnType);
}
