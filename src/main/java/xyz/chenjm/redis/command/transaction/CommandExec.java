package xyz.chenjm.redis.command.transaction;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;

@Command
public class CommandExec implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        return client.exec();
    }
}
