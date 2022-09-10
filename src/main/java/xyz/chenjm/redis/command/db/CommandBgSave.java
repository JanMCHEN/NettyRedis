package xyz.chenjm.redis.command.db;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.command.RedisCommand;

@Command("bgSave")
public class CommandBgSave implements RedisCommand {
    @Override
    public int checkArgs(String... args) {
        if (args.length==1 || args.length==2 && "schedule".equalsIgnoreCase(args[1]))
            return 0;

        return -2;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        return null;
    }
}
