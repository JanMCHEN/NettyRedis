package xyz.chenjm.redis.command.string;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.RedisDB;

@Command("get")
public class CommandGet implements CommandRunner, StringAdapter {
    @Override
    public Object invoke(RedisClient client, String... args) {
        RedisDB db = client.getDb();
        return get(db, args[1]);
    }
}
