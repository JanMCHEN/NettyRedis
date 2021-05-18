package core;

import bin.Server;
import utils.Utils;

import java.util.Arrays;
import java.util.HashMap;

public class CommandMap {

    private static final CommandMap instance = new CommandMap();

    public static CommandMap getInstance() {
        return instance;
    }
    public static RedisObject[] stringsTo(byte[][] args) {
        return stringsTo(args, 0, args.length);
    }
    public static RedisObject[] stringsTo(byte[][] args, int st, int length) {
        RedisObject[] ans = new RedisObject[length];
        for(int i=0;i<ans.length;++i) {
            ans[i] = RedisObject.valueOf(args[i+st]);
        }
        return ans;
    }

    public AbstractCommand get(String s) {
        return commands.get(s);
    }

    private final HashMap<String, AbstractCommand> commands = new HashMap<>();

    public abstract static class AbstractCommand {
        private AbstractCommand() {}
        public boolean isMultiProcess() {
            return false;
        }

        /*
        return
        0:passed,
        -1:wrong number,
        -2:syntax error,
        -3:value not int,
        -4:
         */
        public abstract int checkArgs(byte[] ...args);
        public abstract Object invoke(RedisClient client, byte[] ...args);

    }

    private CommandMap() {
        // db
        commands.put("save", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return RedisDB.saveTask;
            }
        });
        commands.put("bgsave", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                Server.backGroundGroup.submit(RedisDB.saveTask);
                return RedisMessagePool.BG_SAVE;
            }
        });
        commands.put("select", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                try{
                    int i = (int) RedisString.parseLong(args[0], args[0].length);
                    client.setDb(i);
                } catch (NumberFormatException e){
                    throw new RedisException(RedisMessagePool.ERR_SEL);
                }
                return RedisMessagePool.OK;
            }
        });
        commands.put("flushdb", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                client.getDb().flushDb();
                System.gc();
                return RedisMessagePool.OK;
            }
        });
        commands.put("flushall", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisDB.flushAll();
                System.gc();
                return RedisMessagePool.OK;
            }
        });

        // multi
        commands.put("watch", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }

            @Override
            public int checkArgs(byte[]... args) {
                return args.length>0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject[] keys = stringsTo(args);
                return client.watch(keys);
            }
        });
        commands.put("unwatch", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.unwatch();
            }
        });
        commands.put("multi", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.multi();
            }
        });
        commands.put("exec", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.exec();
            }
        });
        commands.put("discard", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.discard();
            }
        });

        // keys
        commands.put("expire", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length!=2) return -1;
                if(Utils.isNumber(args[1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().expire(RedisObject.valueOf(args[0]), RedisString.parseLong(args[1]));
            }
        });
        commands.put("expireat", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length!=2) return -1;
                if(Utils.isNumber(args[1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().expireAt(RedisObject.valueOf(args[0]), RedisString.parseLong(args[1]));
            }
        });
        commands.put("ttl", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().ttl(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("persist", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().persist(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("keys", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().keys(new String(args[0]));
            }
        });
        commands.put("exists", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length>=1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().exists(stringsTo(args));
            }
        });
        commands.put("del", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length>=1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().del(stringsTo(args));
            }
        });
        commands.put("type", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                int type = client.getRedisCommand().type(RedisObject.valueOf(args[0]));
                return RedisMessagePool.TYPES[type];
            }
        });

        // string
        commands.put("get", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject res = client.getRedisCommand().get(RedisObject.valueOf(args[0]));
                if(res==null) return null;
                if(res instanceof RedisString) return res;
                throw RedisException.ERROR_TYPE;
            }
        });
        commands.put("set", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length < 2) {
                    return -1;
                }
                switch (args.length) {
                    case 2:
                        return 0;
                    case 3:
                        if(RedisString.equals(args[2], "nx") || RedisString.equals(args[2], "xx")) {
                            return 0;
                        }
                        return -2;
                    case 4:
                    case 5:
                        int i = 2;
                        if(RedisString.equals(args[2], "nx") || RedisString.equals(args[2], "xx"))  i = 3;
                        else if(args.length==5) {
                            if(!(RedisString.equals(args[2], "nx") || RedisString.equals(args[2], "xx"))) return -2;
                        }

                        if(i+1==args.length) return -1;

                        if(RedisString.equals(args[i], "ex") || RedisString.equals(args[i], "px")){
                            if(Utils.isNumber(args[i+1])) {
                                return 0;
                            }
                            return -3;
                        }
                    default:
                        return -1;
                }
            }
            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                Long time = null;
                boolean nx=false, xx=false;

                if(args.length>2) {
                    switch (Arrays.toString(args[2])) {
                        case "ex":
                            time = RedisString.parseLong(args[3], args[3].length)*1000;
                            break;
                        case "px":
                            time = RedisString.parseLong(args[3], args[3].length);
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
                if(args.length==5) {
                    if(RedisString.equals(args[3], "ex")) {
                        time = RedisString.parseLong(args[3], args[4].length)*1000;
                    }
                    else if (RedisString.equals(args[3], "px")) {
                        time = RedisString.parseLong(args[3], args[4].length);
                    }
                    else if (RedisString.equals(args[4], "xx")) xx = true;
                    else nx = true;
                }
                if(time != null && time <= 0) {
                    return false;
                }
                return client.getRedisCommand().set(key, value, time, nx, xx);
            }
        });
        commands.put("strlen", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().strLen(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("getbit", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 2 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().getBit(RedisObject.valueOf(args[0]), RedisString.parseLong(args[1]));
            }
        });
        commands.put("bitcount", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().bitCount(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("setbit", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length!=3) return -1;
                byte bit = args[2][0];
                if(bit == '0' || bit == '1') return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                long offset = RedisString.parseLong(args[1]);
                int bit = args[2][0] - '0';
                return client.getRedisCommand().setBit(key, offset, bit);
            }
        });
        commands.put("incr", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().increase(RedisObject.valueOf(args[0]), 1);
            }
        });
        commands.put("decr", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 1 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().increase(RedisObject.valueOf(args[0]), -1);
            }
        });
        commands.put("incrby", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 2 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                long v = RedisString.parseLong(args[1]);
                return client.getRedisCommand().increase(RedisObject.valueOf(args[0]), v);
            }
        });
        commands.put("append", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length == 2 ? 0 :-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().append(RedisObject.valueOf(args[0]), args[1]);
            }
        });


        // set
        commands.put("sadd", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length<2) {
                    return -1;
                }
                return 0;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject[] values = new RedisObject[args.length-1];
                boolean isInt = true;
                for (int i=0;i<values.length;++i) {
                    values[i] = RedisObject.valueOf(args[i+1]);
                    if(isInt && values[i] instanceof RedisString.RedisInt) {
                        isInt = false;
                    }
                }
                return client.getRedisCommand().sAdd(key, isInt, values);
            }
        });
        commands.put("srem", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length<2) {
                    return -1;
                }
                return 0;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject[] values = new RedisObject[args.length-1];
                for (int i=0;i<values.length;++i) {
                    values[i] = RedisObject.valueOf(args[i+1]);
                }
                return client.getRedisCommand().sRemove(key, values);
            }
        });
        commands.put("sismember", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length==2) return 0;
                return -1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject member = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().sContain(key, member);
            }
        });
        commands.put("smembers", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().sMembers(key);
            }
        });
        commands.put("scard", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().sCard(key);
            }
        });

        // list
        commands.put("lpush", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length<2?-1:0;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().lPush(RedisObject.valueOf(args[0]), stringsTo(args, 1, args.length-1));
            }
        });
        commands.put("lpushx", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==2?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().lPushX(key, value);
            }
        });
        commands.put("rpush", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length<2?-1:0;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().rPush(RedisObject.valueOf(args[0]), stringsTo(args, 1, args.length-1));
            }
        });
        commands.put("rpushx", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==2?0:-1;
            }
            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().rPushX(key, value);
            }
        });
        commands.put("lpop", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().lPop(key);
            }
        });
        commands.put("rpop", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().rPop(key);
            }
        });
        commands.put("llen", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().lLen(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("lrange", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length!=3) return -1;
                if(Utils.isNumber(args[1]) && Utils.isNumber(args[2])){
                    return 0;
                }
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                long start = RedisString.parseLong(args[1], args[1].length);
                long stop = RedisString.parseLong(args[2], args[2].length);
                return client.getRedisCommand().lRange(key, start, stop);
            }
        });
        commands.put("rpoplpush", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                return args.length==2?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                return client.getRedisCommand().rPopLPush(RedisObject.valueOf(args[0]), RedisObject.valueOf(args[1]));
            }
        });
        commands.put("blpop", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length < 2) return -1;
                if(Utils.isNumber(args[args.length-1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                long timeout;
                try {
                    timeout = RedisString.parseLong(args[args.length - 1]);
                    if (timeout < 0) {
                        throw new RedisException("ERR timeout is negative");
                    }
                }catch (NumberFormatException e){
                    throw new RedisException("ERR timeout is not an integer or out of range");
                }
                RedisObject[] keys = stringsTo(args, 0, args.length-1);
                Object ans = client.getRedisCommand().bLPop(keys);
                if(ans==null){
                    // multi过程不阻塞
                    if(client.isMulti()){
                        return null;
                    }
                    client.blocked(timeout*1000, null, keys);
                    throw new RedisException();
                }
                else {
                    return ans;
                }
            }
        });
        commands.put("brpop", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length < 2) return -1;
                if(Utils.isNumber(args[args.length-1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                long timeout;
                try {
                    timeout = RedisString.parseLong(args[args.length - 1]);
                    if (timeout < 0) {
                        throw new RedisException("ERR timeout is negative");
                    }
                }catch (NumberFormatException e){
                    throw new RedisException("ERR timeout is not an integer or out of range");
                }
                RedisObject[] keys = stringsTo(args, 0, args.length-1);
                Object ans = client.getRedisCommand().bRPop(keys);
                if(ans==null){
                    // multi过程不阻塞
                    if(client.isMulti()){
                        return null;
                    }
                    client.blocked(timeout*1000, null, keys);
                    throw new RedisException();
                }
                else {
                    return ans;
                }
            }
        });
        commands.put("brpoplpush", new AbstractCommand() {
            @Override
            public int checkArgs(byte[]... args) {
                if(args.length != 3) return -1;
                if(Utils.isNumber(args[2])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, byte[]... args) {
                long timeout;
                try {
                    timeout = RedisString.parseLong(args[2]);
                    if (timeout < 0) {
                        throw new RedisException("ERR timeout is negative");
                    }
                }catch (NumberFormatException e){
                    throw new RedisException("ERR timeout is not an integer or out of range");
                }
                RedisObject source = RedisObject.valueOf(args[0]);
                RedisObject target = RedisObject.valueOf(args[1]);
                Object ans = client.getRedisCommand().bRPopLPush(source, target);
                if(ans==null){
                    // multi过程不阻塞
                    if(client.isMulti()){
                        return null;
                    }
                    client.blocked(timeout*1000, target, source);
                    throw new RedisException();
                }
                else {
                    return ans;
                }
            }
        });
    }
}
