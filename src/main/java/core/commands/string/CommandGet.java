package core.commands.string;

import annotation.Command;
import core.RedisClient;
import core.RedisCommand;
import core.RedisDB;

@Command("get")
public class CommandGet implements RedisCommand {
    @Override
    public int checkArgs(String... args) {
        return args.length == 2? 0:-1;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        RedisDB db = client.getDb();
        return db.getString(args[0]);
    }
}
