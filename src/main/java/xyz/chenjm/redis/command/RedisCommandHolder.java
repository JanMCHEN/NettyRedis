package xyz.chenjm.redis.command;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.core.RedisClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * redis命令管理
 */
public interface RedisCommandHolder extends RedisCommandAround{
    RedisCommand getCommand(String key);
    void addCommand(String key, RedisCommand command);

    default void addCommand(Class<RedisCommand> cls) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<RedisCommand> constructor = cls.getConstructor();
        RedisCommand redisCommand = constructor.newInstance();
        Command annotation = cls.getAnnotation(Command.class);
        String key = annotation==null? "": annotation.value();

        // 未指定 name,由类名决定
        if (key.isEmpty()) {
            key = cls.getSimpleName();
            if (key.startsWith("Command")) {
                key = key.substring(7);
            }
        }
        addCommand(key, redisCommand);
    }

    default Object call(RedisCommand cmd, RedisClient client, String ...args) {
        before(client, args);
        Object res = cmd.invoke(client, args);
        res = after(res, client, args);
        return res;
    }
}
