package xyz.chenjm.redis.command.string;

import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.RedisDB;
import xyz.chenjm.redis.core.structure.RedisString;
import xyz.chenjm.redis.exception.SyntaxException;
import xyz.chenjm.redis.exception.ValueNotIntException;

@Command(value = "set", readonly = false, args = -2)
public class CommandSet implements CommandRunner {
    @Override
    public Object invoke(RedisClient client, String... args) {
        String key = args[1];
        RedisString value = RedisString.newString(args[2]);

        long time = 1;
        boolean nx=false, xx=false;
        int timeAt = -1;

        boolean syntaxErr = false;

        int n = args.length;

        switch (n) {
            case 3:
                break;
            case 4:
                if("nx".equals(args[3])) {
                    nx = true;
                } else if ("xx".equals(args[3])) {
                    xx = true;
                }else {
                    syntaxErr = true;
                }
                break;
            case 5:
                if("ex".equals(args[3])) {
                    timeAt = 4;
                    time *= 1000;
                } else if ("px".equals(args[3])) {
                    timeAt = 4;
                }else {
                    syntaxErr = true;
                }
                break;
            case 6:
                if("ex".equals(args[3]) || "px".equals(args[3])) {
                    timeAt = 4;
                    if ("ex".equals(args[3]))
                        time*=1000;
                    nx = "nx".equals(args[5]);
                    xx = !nx &&  "xx".equals(args[5]);
                    if (!(nx || xx)) {
                        syntaxErr = true;
                    }
                }
                else if("ex".equals(args[4]) || "px".equals(args[4])) {
                    timeAt = 5;
                    if ("ex".equals(args[4]))
                        time*=1000;
                    nx = "nx".equals(args[3]);
                    xx = !nx &&  "xx".equals(args[3]);
                    if (!(nx || xx)) {
                        syntaxErr = true;
                    }
                }
                else {
                    syntaxErr = true;
                }
                break;
            default:
                syntaxErr = true;
        }

        if (syntaxErr) {
            throw new SyntaxException();
        }

        if(timeAt >= 0) {
            try {
                time *= Long.parseLong(args[timeAt]);
            } catch (NumberFormatException e) {
                throw new ValueNotIntException();
            }
        }
        RedisDB db = client.getDb();
        boolean hasSet = true;
        if (nx) {
            Object o = db.getAndDelete(key);
            if (o == null)
                db.set(key, value);
            else
                hasSet = false;
        }
        else if(xx) {
            Object o = db.getAndDelete(key);
            if (o != null)
                db.set(key, value);
            else
                hasSet = false;
        }
        else
            db.set(key, value);

        if (timeAt>=0 && hasSet) {
            db.expireAt(key, time+System.currentTimeMillis(), false);
        }

        return hasSet;
    }

}
