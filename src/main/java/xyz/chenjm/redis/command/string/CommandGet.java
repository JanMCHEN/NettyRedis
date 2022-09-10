package xyz.chenjm.redis.command.string;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.core.RedisDB;
import xyz.chenjm.redis.core.RedisDB2;

@Command("get")
public class CommandGet implements RedisCommand, StringAdapter {
    @Override
    public int checkArgs(String... args) {
        return args.length == 2? 0:-1;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        RedisDB db = client.getDb();
        return get(db, args[1]);
    }
}
