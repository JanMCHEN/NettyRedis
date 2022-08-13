package core;

import java.util.Map;

public class DefaultCommandFactory implements CommandFactory{
    private final Map<String, RedisCommand<?>> commandMap = new RedisDict<>();
    @Override
    public RedisCommand<?> getCommand(String key) {
        return commandMap.get(key);
    }

    @Override
    @SuppressWarnings("all")
    public <T> RedisCommand<T> getCommand(String key, Class<T> returnType) {
        return (RedisCommand<T>) commandMap.get(key);
    }

    public void addCommand(String key, RedisCommand<?> command) {
        commandMap.put(key, command);
    }

    public void refresh() {

    }
}