package xyz.chenjm.redis.command.db;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;

@Command("bgSave")
public class CommandBgSave implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        return true;
    }
}
