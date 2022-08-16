package core.commands.string;

import annotation.Command;
import core.RedisClient;
import core.RedisCommand;
import core.RedisDB;
import core.exception.ErrorIntException;
import core.structure.RedisString;

@Command("set")
public class CommandSet implements RedisCommand {
    /**
     * set
     * @param args set key value nx|xx ex|px v
     * @return 0
     */
    @Override
    public int checkArgs(String... args) {
        int n = args.length;
        if(n < 3) {
            return -1;
        }
        switch (n) {
            case 3:
                return 0;
            case 4:
                if("nx".equals(args[3]) || "xx".equals(args[3])) {
                    return 0;
                }
                return -2;
            case 5:
                if("ex".equals(args[3]) || "px".equals(args[3])) {
                    return 0;
                }
                return -2;
            case 6:
                if("ex".equals(args[3]) || "px".equals(args[3])) {
                    if("nx".equals(args[5]) || "xx".equals(args[5])) {
                        return 0;
                    }
                }
                if("ex".equals(args[4]) || "px".equals(args[4])) {
                    if("nx".equals(args[3]) || "xx".equals(args[3])) {
                        return 0;
                    }
                }
            default:
                return -2;
        }
    }
    @Override
    public Object invoke(RedisClient client, String... args) {
        String key = args[1];
        RedisString value = RedisString.newString(args[2]);

        long time = 1;
        boolean nx=false, xx=false;
        int timeAt = -1;

        if(args.length>3) {
            switch (args[3]) {
                case "ex":
                    timeAt = 3;
                    time = 1000;
                    break;
                case "px":
                    timeAt = 3;
                    break;
                case "nx":
                    nx = true;
                    break;
                case "xx":
                    xx = true;
                    break;
                default:
                    break;
            }
        }
        if(args.length==6) {
            if("ex".equals(args[4])) {
                timeAt = 4;
                time = 1000;
            }
            else if ("px".equals(args[4])) {
                timeAt = 4;
            }
            else if ("xx".equals(args[5])) xx = true;
            else nx = true;
        }
        if(timeAt >= 0) {
            try {
                time *= Long.parseLong(args[timeAt]);
            } catch (NumberFormatException e) {
                throw new ErrorIntException();
            }
        }
        RedisDB db = client.getDb();
        boolean hasSet = true;
        if (nx) hasSet = db.setIfAbsent(key, value);
        else if(xx) hasSet = db.setIfPresent(key, value);
        else db.set(key, value);
        if (timeAt>=0 && hasSet) {
            db.expire(key, time, false);
        }

        return hasSet;
    }

}
