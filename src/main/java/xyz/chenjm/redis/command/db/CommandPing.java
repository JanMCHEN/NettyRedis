package xyz.chenjm.redis.command.db;

import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;

@Command(value = "ping", args = 0)
public class CommandPing implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        return new SimpleStringRedisMessage("pong");
    }
}
