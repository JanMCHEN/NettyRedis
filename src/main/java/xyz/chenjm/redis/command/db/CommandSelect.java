package xyz.chenjm.redis.command.db;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.exception.RedisException;
import xyz.chenjm.redis.exception.ValueNotIntException;

@Command("select")
public class CommandSelect implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        try{
            int i = Integer.parseInt(args[1]);
            client.selectDb(i);

        }catch (NumberFormatException e) {
            throw new ValueNotIntException();
        }catch (IndexOutOfBoundsException ix) {
            throw new RedisException("ERR DB index is out of range");
        }
        return true;
    }
}
