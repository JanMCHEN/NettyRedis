package core;

import bin.Server;
import utils.Utils;
import java.util.HashMap;

public class CommandMap {

    private static final CommandMap instance = new CommandMap();

    public static CommandMap getInstance() {
        return instance;
    }
    public static RedisObject[] stringsTo(String[] args) {
        return stringsTo(args, 0, args.length);
    }
    public static RedisObject[] stringsTo(String[] args, int st, int length) {
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
        public abstract int checkArgs(String ...args);
        public abstract Object invoke(RedisClient client, String ...args);

    }

    private CommandMap() {
        // db
        commands.put("save", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return RedisDB.saveTask;
            }
        });
        commands.put("bgsave", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                Server.backGroundGroup.submit(RedisDB.saveTask);
                return RedisMessagePool.BG_SAVE;
            }
        });
        commands.put("select", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length == 1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                try{
                    int i = Integer.parseInt(args[0]);
                    client.setDb(i);
                } catch (NumberFormatException e){
                    throw new RedisException(RedisMessagePool.ERR_SEL);
                }
                return RedisMessagePool.OK;
            }
        });
        commands.put("flushdb", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                client.getDb().flushDb();
                System.gc();
                return RedisMessagePool.OK;
            }
        });
        commands.put("flushall", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
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
            public int checkArgs(String... args) {
                return args.length>0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject[] keys = stringsTo(args);
                return client.watch(keys);
            }
        });
        commands.put("unwatch", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.unwatch();
            }
        });
        commands.put("multi", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(String... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.multi();
            }
        });
        commands.put("exec", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(String... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.exec();
            }
        });
        commands.put("discard", new AbstractCommand() {
            @Override
            public boolean isMultiProcess() {
                return true;
            }
            @Override
            public int checkArgs(String... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.discard();
            }
        });

        // keys
        commands.put("expire", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length!=2) return -1;
                if(Utils.isNumber(args[1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().expire(RedisObject.valueOf(args[0]), Long.parseLong(args[1]));
            }
        });
        commands.put("expireat", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length!=2) return -1;
                if(Utils.isNumber(args[1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().expireAt(RedisObject.valueOf(args[0]), Long.parseLong(args[1]));
            }
        });
        commands.put("ttl", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().ttl(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("persist", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().persist(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("keys", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().keys(args[0]);
            }
        });
        commands.put("exists", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length>=1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().exists(stringsTo(args));
            }
        });
        commands.put("del", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length>=1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().del(stringsTo(args));
            }
        });
        commands.put("type", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                int type = client.getRedisCommand().type(RedisObject.valueOf(args[0]));
                return RedisObject.TYPES[type];
            }
        });

        // string
        commands.put("get", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length == 1? 0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject res = client.getRedisCommand().get(RedisObject.valueOf(args[0]));
                if(res==null) return null;
                if(res.isString()) return res;
                throw RedisException.ERROR_TYPE;
            }
        });
        commands.put("set", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length < 2) {
                    return -1;
                }
                switch (args.length) {
                    case 2:
                        return 0;
                    case 3:
                        if("nx".equals(args[2]) || "xx".equals(args[2])) {
                            return 0;
                        }
                        return -2;
                    case 4:
                    case 5:
                        int i = 2;
                        if("nx".equals(args[2]) || "xx".equals(args[2]))  i = 3;
                        else if(args.length==5) {
                            if(!("nx".equals(args[4]) || "xx".equals(args[4]))) return -2;
                        }

                        if(i+1==args.length) return -1;

                        if("ex".equals(args[i]) || "px".equals(args[i])){
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
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                Long time = null;
                boolean nx=false, xx=false;

                if(args.length>2) {
                    switch (args[2]) {
                        case "ex":
                            time = Long.parseLong(args[3])*1000;
                            break;
                        case "px":
                            time = Long.parseLong(args[3]);
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
                    if(args[3].equals("ex")) {
                        time = Long.parseLong(args[4])*1000;
                    }
                    else if (args[3].equals("px")) {
                        time = Long.parseLong(args[4]);
                    }
                    else if (args[4].equals("xx")) xx = true;
                    else nx = true;
                }
                if(time != null && time <= 0) {
                    return false;
                }
                return client.getRedisCommand().set(key, value, time, nx, xx);
            }
        });

        // set
        commands.put("sadd", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length<2) {
                    return -1;
                }
                return 0;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject[] values = new RedisObject[args.length-1];
                boolean isInt = true;
                for (int i=0;i<values.length;++i) {
                    values[i] = RedisObject.valueOf(args[i+1]);
                    if(isInt && !values[i].isEncodeInt()) {
                        isInt = false;
                    }
                }
                return client.getRedisCommand().sAdd(key, isInt, values);
            }
        });
        commands.put("srem", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length<2) {
                    return -1;
                }
                return 0;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
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
            public int checkArgs(String... args) {
                if(args.length==2) return 0;
                return -1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject member = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().sContain(key, member);
            }
        });
        commands.put("smembers", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().sMembers(key);
            }
        });
        commands.put("scard", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().sCard(key);
            }
        });

        // list
        commands.put("lpush", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length<2?-1:0;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().lPush(RedisObject.valueOf(args[0]), stringsTo(args, 1, args.length-1));
            }
        });
        commands.put("lpushx", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==2?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().lPushX(key, value);
            }
        });
        commands.put("rpush", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length<2?-1:0;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().rPush(RedisObject.valueOf(args[0]), stringsTo(args, 1, args.length-1));
            }
        });
        commands.put("rpushx", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==2?0:-1;
            }
            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().rPushX(key, value);
            }
        });
        commands.put("lpop", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().lPop(key);
            }
        });
        commands.put("rpop", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==1?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                return client.getRedisCommand().rPop(key);
            }
        });
        commands.put("llen", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==0?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().lLen(RedisObject.valueOf(args[0]));
            }
        });
        commands.put("lrange", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length!=3) return -1;
                if(Utils.isNumber(args[1]) && Utils.isNumber(args[2])){
                    return 0;
                }
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                long start = Long.parseLong(args[1]);
                long stop = Long.parseLong(args[2]);
                return client.getRedisCommand().lRange(key, start, stop);
            }
        });
        commands.put("rpoplpush", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length==2?0:-1;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                return client.getRedisCommand().rPopLPush(RedisObject.valueOf(args[0]), RedisObject.valueOf(args[1]));
            }
        });
        commands.put("blpop", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                if(args.length < 2) return -1;
                if(Utils.isNumber(args[args.length-1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                long timeout;
                try {
                    timeout = Long.parseLong(args[args.length - 1]);
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
            public int checkArgs(String... args) {
                if(args.length < 2) return -1;
                if(Utils.isNumber(args[args.length-1])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                long timeout;
                try {
                    timeout = Long.parseLong(args[args.length - 1]);
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
            public int checkArgs(String... args) {
                if(args.length != 3) return -1;
                if(Utils.isNumber(args[2])) return 0;
                return -3;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                long timeout;
                try {
                    timeout = Long.parseLong(args[2]);
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

    public static void main(String[] args){
        CommandMap instance = getInstance();
        System.out.println(RedisDB.getDB(0).getDict().toString());
        System.out.println(instance.get("get").checkArgs("a"));
        System.out.println(instance.get("set").checkArgs("a", "b", "nx", "ex","100"));
    }
}
