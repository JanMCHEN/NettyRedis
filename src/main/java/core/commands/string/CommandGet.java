package core.commands.string;

import annotation.Command;
import core.RedisClient;
import core.RedisCommand;
import core.RedisDB;
import core.exception.RedisException;
import core.structure.RedisObject;
import core.structure.RedisString;

@Command("get")
public class CommandGet implements RedisCommand {
    @Override
    public int checkArgs(byte[]... args) {
        return args.length == 1? 0:-1;
    }

    @Override
    public Object invoke(RedisClient client, byte[]... args) {
//        RedisObject res = client.getRedisCommand().get(RedisObject.valueOf(args[0]));
//        RedisDB db = client.getDb();
//        if(res==null) return null;
//        if(res instanceof RedisString) return res;
        throw RedisException.ERROR_TYPE;
    }
}
