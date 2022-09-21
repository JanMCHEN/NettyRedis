package xyz.chenjm.redis.command.replicate;

import xyz.chenjm.redis.command.RedisCommand1;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.exception.ErrorIntException;

public class CommandSlaveOf implements RedisCommand1 {
    @Override
    public int checkArgs(String... args) {
        return args.length == 3 ? 0: -1;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        String host = args[1];
        int port;
        try {
            port = Integer.parseInt(args[2]);
        }catch (NumberFormatException e) {
            throw new ErrorIntException();
        }

        return true;

    }
}
