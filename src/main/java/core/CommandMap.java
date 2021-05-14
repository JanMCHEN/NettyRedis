package core;

import utils.Utils;
import java.util.HashMap;

public class CommandMap {

    private static final CommandMap instance = new CommandMap();

    public static CommandMap getInstance() {
        return instance;
    }
    public static RedisObject[] stringsTo(String[] args) {
        RedisObject[] ans = new RedisObject[args.length];
        for(int i=0;i<ans.length;++i) {
            ans[i] = RedisObject.valueOf(args[i]);
        }
        return ans;
    }

    public AbstractCommand get(String s) {
        return commands.get(s);
    }

    private final HashMap<String, AbstractCommand> commands = new HashMap<>();

    public abstract class AbstractCommand {
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

        commands.put("lpush", new AbstractCommand() {
            @Override
            public int checkArgs(String... args) {
                return args.length<2?-1:0;
            }

            @Override
            public Object invoke(RedisClient client, String... args) {
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().lPush(key, value);
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
                RedisObject key = RedisObject.valueOf(args[0]);
                RedisObject value = RedisObject.valueOf(args[1]);
                return client.getRedisCommand().rPush(key, value);
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


        // list


//        try {
//            command.put("get", new AbComM("get", RedisObject.class) {
//                @Override
//                Object [] CommandArgParse(String... args) {
//                    Object [] res = new Object[1];
//                    res[0] = RedisObject.valueOf(args[0]);
//                    return res;
//                }
//                @Override
//                public int checkArgs(String... args) {
//                    if(args == null || args.length != 1) {
//                        return -1;
//                    }
//                    return 0;
//                }
//            });
//            command.put("set", new AbComM("set", RedisObject.class,
//                    RedisObject.class, Long.class, boolean.class, boolean.class) {
//                @Override
//                Object [] CommandArgParse(String... args) {
//                    Object [] res = new Object[5];
//                    res[0] = RedisObject.valueOf(args[0]);
//                    res[1] = RedisObject.valueOf(args[1]);
//                    res[2] = null;
//                    res[3] = false;
//                    res[4] = false;
//
//                    if(args.length==2) return res;
//                    switch (args[2]) {
//                        case "ex":
//                            res[2] = Long.parseLong(args[3])*1000;
//                            break;
//                        case "px":
//                            res[2] = Long.parseLong(args[3]);
//                            break;
//                        case "nx":
//                            res[3] = true;
//                            break;
//                        case "xx":
//                            res[4] = true;
//                            break;
//                        default:
//                            break;
//                    }
//
//                    if(args.length==5) {
//                        if(args[3].equals("ex")) {
//                            res[2] = Long.parseLong(args[4])*1000;
//                        }
//                        else if (args[3].equals("px")) {
//                            res[2] = Long.parseLong(args[4]);
//                        }
//                        else if (args[4].equals("xx")) res[4] = true;
//                        else res[3] = true;
//                    }
//                    return res;
//                }
//                @Override
//                public int checkArgs(String... args) {
//                    if(args == null || args.length < 2) {
//                        return -1;
//                    }
//                    switch (args.length) {
//                        case 2:
//                            return 0;
//                        case 3:
//                            if("nx".equals(args[2]) || "xx".equals(args[2])) {
//                                return 0;
//                            }
//                            return -2;
//                        case 4:
//                        case 5:
//                            int i = 2;
//                            if("nx".equals(args[2]) || "xx".equals(args[2]))  i = 3;
//                            else if(args.length==5) {
//                                if(!("nx".equals(args[4]) || "xx".equals(args[4]))) return -2;
//                            }
//
//                            if(i+1==args.length) return -1;
//
//                            if("ex".equals(args[i]) || "px".equals(args[i])){
//                                if(Utils.isNumber(args[i+1])) {
//                                    return 0;
//                                }
//                                return -3;
//                            }
//                        default:
//                            return -1;
//                    }
//                }
//
//                @Override
//                public Object checkRes(Object res) {
//                    if(res==Boolean.FALSE) {
//                        return RedisMessagePool.NULL;
//                    }
//                    return res;
//                }
//            });
//            command.put("expire", new AbComM("expire", RedisObject.class, long.class) {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    Object[] res = new Object[2];
//                    res[0] = RedisObject.valueOf(args[0]);
//                    res[1] = Long.parseLong(args[1]);
//                    return res;
//                }
//
//                @Override
//                public int checkArgs(String... args) {
//                    if(args==null || args.length!=2) return -1;
//                    if(Utils.isNumber(args[1])) return 0;
//                    return -3;
//                }
//            });
//
//            command.put("keys", new AbComM("keys", String.class) {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    return args;
//                }
//
//                @Override
//                public int checkArgs(String... args) {
//                    if(args == null || args.length != 1) {
//                        return -1;
//                    }
//                    return 0;
//                }
//            });
//
//            command.put("watch", new AbComM(true,"watch", ClientStatus.class, RedisObject[].class) {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    Object[] ans = new Object[args.length+1];
//                    for(int i=0;i<args.length;++i) {
//                        ans[i+1] = RedisObject.valueOf(args[i]);
//                    }
//                    return ans;
//                }
//
//                @Override
//                public int checkArgs(String... args) {
//                    if(args==null || args.length==0) {
//                        return -1;
//                    }
//                    return 0;
//                }
//
//                @Override
//                public Object checkRes(Object res) {
//                    if (res==Boolean.FALSE) {
//                        return RedisMessagePool.ERR_WATCH;
//                    }
//                    return RedisMessagePool.OK;
//                }
//
//            });
//            command.put("unwatch", new AbComM(false,"unwatch") {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    return new Object[0];
//                }
//                @Override
//                public int checkArgs(String... args) {
//                    if(args==null || args.length==0) return 0;
//                    return -1;
//                }
//                @Override
//                public Object invoke(ClientStatus client, Object obj, String... args) throws InvocationTargetException, IllegalAccessException {
//                    client.reset();
//                    return super.invoke(client, obj, args);
//                }
//            });
//            command.put("exec", new AbComM(true, "exec", ClientStatus.class) {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    return new Object[0];
//                }
//
//                @Override
//                public int checkArgs(String... args) {
//                    if(args==null || args.length==0) return 0;
//                    return -1;
//                }
//            });
//            command.put("multi", new AbComM(true, "exec", ClientStatus.class) {
//                @Override
//                Object[] CommandArgParse(String... args) {
//                    return new Object[0];
//                }
//
//                @Override
//                public int checkArgs(String... args) {
//                    if(args==null || args.length==0) return 0;
//                    return -1;
//                }
//            });
//
//        } catch (NoSuchMethodException e) {
//            System.out.println("command fail:"+e.getMessage());
//        }

    }

    public static void main(String[] args){
        CommandMap instance = getInstance();
        System.out.println(RedisDB.getDB(0).getDict().toString());
        System.out.println(instance.get("get").checkArgs("a"));
        System.out.println(instance.get("set").checkArgs("a", "b", "nx", "ex","100"));
    }
}
