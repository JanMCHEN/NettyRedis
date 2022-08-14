package core;

import core.exception.RedisException;
import core.structure.RedisObject;
import io.netty.channel.ChannelHandlerContext;

import java.util.*;

public class RedisClient {

    private class CommandMetaData {
        private final RedisCommand method;
        private final byte[][]args;

        public CommandMetaData(RedisCommand method, byte[][] args) {
            this.method = method;
            this.args = args;
        }

        public Object call(){
            return method.invoke(RedisClient.this, args);
        }
    }

    private byte state = 0;
    private final List<CommandMetaData> commands = new LinkedList<>();
    private final HashSet<RedisObject> watchedKeys = new HashSet<>();
    private RedisDB db;
    private final ChannelHandlerContext ctx;

    // blocked
    long timeout;
    List<RedisObject> blockedKeys = new LinkedList<>();
    RedisObject target;


    public RedisClient(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        db = RedisDB.getDB(0);
    }
    public RedisDB getDb() {
        return db;
    }
    public void setDb(int i) {
        if(i>=0 && i < RedisDB.dbs.length) {
            db = RedisDB.getDB(i);
        }
        else {
            throw new RedisException(RedisMessagePool.ERR_SEL);
        }
    }

    public RedisDB.RedisCommand getRedisCommand() {
        return db.getRedisCommand();
    }

    public boolean isMulti() {
        return (state & 1) == 1;
    }
    public boolean isDirty() {
        return (state >> 1 & 1) == 1;
    }
    public boolean isError() {
        return (state >> 2 & 1) == 1;
    }
    public boolean isBlocked(){
        return (state >> 3 & 1) == 1;
    }

    public boolean isNormal() {
        return state == 0;
    }

    public void setMulti() {
        state = (byte) (state | 1);
    }
    public void setDirty() {
        state = (byte) (state | 2);
    }
    public void setError() {
        if(isMulti()) {
            state = (byte) (state | 4);
        }
    }
    public void setBlocked() {
        state = (byte) (state | 8);
    }

    public void addCommand(RedisCommand method ,byte[][] args) {
        if(isError()||isDirty())
            return;
        commands.add(new CommandMetaData(method, args));
    }

    public Object watch(RedisObject...keys) {
        if(!isNormal()) return RedisMessagePool.ERR_WATCH;
        for(RedisObject key:keys) {
            watchedKeys.add(key);
            db.watchAdd(this, key);
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
        else if(isDirty()) {
            res = RedisMessagePool.NULL;
        }
        else if(isNormal()) {
            res = RedisMessagePool.ERR_EXEC_MUL;
        }
        else if(isMulti()) {
            Iterator<CommandMetaData> iterator = commands.iterator();
            List<Object> resL = new LinkedList<>();
            while(iterator.hasNext()) {
                CommandMetaData next = iterator.next();
                Object ans = null;
                try {
                    ans = next.call();
                } catch (RedisException e){
                    ans = e.redisMessage;
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    iterator.remove();
                }
                resL.add(ans);
            }
            res = resL.toArray();
        }
        reset();
        return res;
    }
    public Object multi(){
        if(isMulti()) return RedisMessagePool.ERR_MULTI;
        setMulti();
        return RedisMessagePool.OK;
    }
    public Object discard() {
        if(!isMulti()) return RedisMessagePool.ERR_DISCARD;
        reset();
        return RedisMessagePool.OK;
    }
    public void reset() {
        state &= 8;
        commands.clear();
        Iterator<RedisObject> iterator = watchedKeys.iterator();
        while(iterator.hasNext()) {
            RedisObject key = iterator.next();
            db.watchRemove(this, key);
            iterator.remove();
        }
    }

    public void blocked(long timeout, RedisObject target, RedisObject ...keys) {
        if(keys.length==0) return;
        unblocked();
        setBlocked();
        this.timeout = timeout==0? timeout: System.currentTimeMillis() + timeout;
        this.target = target;
        for(RedisObject key:keys) {
            blockedKeys.add(key);
            db.blockedAdd(this, key);
        }
    }
    public void unblocked() {
        unblocked(true);
    }
    public void unblocked(boolean checkDb) {
        state &= 7;
        timeout = -1;
        target = null;
        Iterator<RedisObject> iterator = blockedKeys.iterator();
        if(!checkDb) {
            blockedKeys.clear();
            return;
        }
        while (iterator.hasNext()) {
            db.blockedRemove(this, iterator.next());
            iterator.remove();
        }
    }
    public void blockedExec(RedisObject key) {
        unblocked(false);
        RedisObject val = db.getRedisCommand().lPop(key);
        if(target!=null) {
            try {
                db.getRedisCommand().lPush(target, val);
            }catch (RedisException e) {
                writeAndFlush(e.redisMessage);
                // push失败 target不是list类型，push回去
                db.getRedisCommand().lPush(key, val);
                return;
            }
        }
        writeAndFlush(new Object[]{key, val});
    }
    public void writeAndFlush(Object msg) {
        if (msg==null) {
            msg = RedisMessagePool.NULL;
        }
        ctx.writeAndFlush(msg);
    }
    public void close() {
        reset();
        unblocked();
    }
}
