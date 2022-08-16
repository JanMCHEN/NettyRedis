package core;

public interface RedisCommandHolder extends RedisCommandAround{
    RedisCommand getCommand(String key);
    void addCommand(String key, RedisCommand command);

    default Object call(RedisCommand cmd, RedisClient client, String ...args) {
        before(client, args);
        Object res = cmd.invoke(client, args);
        res = after(res, client, args);
        return res;
    }

}
