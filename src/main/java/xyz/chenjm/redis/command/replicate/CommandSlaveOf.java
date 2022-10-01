package xyz.chenjm.redis.command.replicate;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.command.RedisCommand1;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.exception.ErrorIntException;

@Command(value = "slaveOf", args = 2)
public class CommandSlaveOf implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        if ("no".equals(args[1]) && "one".equals(args[2])) {
            client.getServer().setSlave("", 0);
        }
        else {
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            client.getServer().setSlave(host, port);
        }
        return true;
    }
}
