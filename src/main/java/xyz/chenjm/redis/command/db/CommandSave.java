package xyz.chenjm.redis.command.db;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.core.RedisClient;

@Command("save")
public class CommandSave implements RedisCommand {
    @Override
    public int checkArgs(String... args) {
        return args.length==1? 0:-1;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        return null;
    }
}
