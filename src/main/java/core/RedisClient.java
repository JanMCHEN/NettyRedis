package core;

import io.netty.handler.codec.redis.RedisMessage;

import java.util.*;

public class RedisClient {

    private class Command {
        private final CommandMap.AbstractCommand method;
        private String []args;

        public Command(CommandMap.AbstractCommand method, String[] args) {
            this.method = method;
            this.args = args;
        }

        public Object call(){
            return method.invoke(RedisClient.this, args);
        }
    }
    private byte state;
    private List<Command> commands;
    private HashSet<RedisObject> watchedKeys;
    private RedisDB db;

    public RedisClient() {
        state = 0;
        commands = new LinkedList<>();
        watchedKeys = new HashSet<>();
        db = RedisDB.getDB(0);
    }

    public RedisDB getDb() {
        return db;
    }
    public RedisDB.RedisCommand getRedisCommand() {
        return db.getRedisCommand();
    }

    public boolean isMulti() {
        return state == 1;
    }
    public boolean isDirty() {
        return state >> 1 == 1;
    }
    public boolean isError() {
        return state >> 2 == 1;
    }

    public boolean isNormal() {
        return state == 0;
    }

    public boolean setMulti() {
        if(isMulti()) {
            return false;
        }
        state = (byte) (state | 1);
        return true;
    }
    public boolean setDirty() {
        state = (byte) (state | 2);
        return true;
    }

    public boolean setError() {
        if(isMulti()) {
            state = (byte) (state | 4);
            return true;
        }
        return false;
    }

    public void addCommand(CommandMap.AbstractCommand method,String[] args) {
        if(isError()||isDirty())
            return;
        commands.add(new Command(method, args));
    }

    public Object watch(RedisObject...keys) {
        if(!isNormal()) return RedisMessagePool.ERR_WATCH;
        for(var key:keys) {
            watchedKeys.add(key);
            db.watch_add(this, key);
        }
        watchedKeys.addAll(Arrays.asList(keys));
        return RedisMessagePool.OK;
    }
    public Object unwatch() {
        if(!isMulti()) reset();
        // 如果在执行事务过程中执行了unwatch,不进行任何动作，不知道官方为啥要把这条命令在multi中执行
        return RedisMessagePool.OK;
    }
    public Object exec() {
        Object res=null;
        if(isError()) {
            res = RedisMessagePool.ERR_EXEC_ERR;
        }
        else if(isNormal()) {
            res = RedisMessagePool.ERR_EXEC_MUL;
        }
        else if(isMulti()) {
            Iterator<Command> iterator = commands.iterator();
            List<Object> resL = new LinkedList<>();
            while(iterator.hasNext()) {
                Command next = iterator.next();
                try {
                    Object ans = next.call();
                    resL.add(ans);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    iterator.remove();
                }
            }
            res = resL;
        }
        reset();
        return res;
    }
    public Object multi(){
        if(setMulti()) return RedisMessagePool.OK;
        else return RedisMessagePool.ERR_MULTI;
    }
    public Object discard() {
        if(!isMulti()) return RedisMessagePool.ERR_DISCARD;
        reset();
        return RedisMessagePool.OK;
    }
    public void reset() {
        state = 0;
        commands.clear();
        Iterator<RedisObject> iterator = watchedKeys.iterator();
        while(iterator.hasNext()) {
            RedisObject key = iterator.next();
            db.watch_remove(this, key);
            iterator.remove();
        }
    }
}
