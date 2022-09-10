package xyz.chenjm.redis.core;

import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.exception.RedisException;
import xyz.chenjm.redis.core.structure.RedisObject;
import io.netty.channel.ChannelHandlerContext;

import java.util.*;

/**
 * 客户端状态管理
 */
public class RedisClient {

    private class CommandMetaData {
        private final RedisCommand method;
        private final String[]args;

        public CommandMetaData(RedisCommand method, String[] args) {
            this.method = method;
            this.args = args;
        }

        public Object call(){
            return method.invoke(RedisClient.this, args);
        }
    }

    private byte state = 0;
    private final List<CommandMetaData> commands = new LinkedList<>();
    private final HashSet<String> watchedKeys = new HashSet<>();
    private RedisDB db;
    private final ChannelHandlerContext ctx;

    // blocked
    long timeout;
    List<String> blockedKeys = new LinkedList<>();
    RedisObject target;


    public RedisClient(ChannelHandlerContext ctx) {
        this.ctx = ctx;
//        db = RedisDB.getDB(0);
    }
    public RedisDB getDb() {
        return db;
    }
    public void setDb(int i) {
        if(i>=0 && i < RedisDB2.dbs.length) {
//            db = RedisDB.getDB(i);
        }
        else {
            throw new RedisException(RedisMessageFactory.ERR_SEL);
        }
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

    public void addCommand(RedisCommand method ,String[] args) {
        if(isError()||isDirty())
            return;
        commands.add(new CommandMetaData(method, args));
    }

    public Object watch(String...keys) {
        if(!isNormal()) return RedisMessageFactory.ERR_WATCH;
        for(String key:keys) {
            watchedKeys.add(key);
            db.watchAdd(this, key);
        }
        watchedKeys.addAll(Arrays.asList(keys));
        return RedisMessageFactory.OK;
    }
    public Object unwatch() {
        if(!isMulti()) reset();
        // 如果在执行事务过程中执行了unwatch,不进行任何动作，不知道官方为啥要把这条命令在multi中执行
        return RedisMessageFactory.OK;
    }
    public Object exec() {
        Object res=null;
        if(isError()) {
            res = RedisMessageFactory.ERR_EXEC_ERR;
        }
        else if(isDirty()) {
            res = RedisMessageFactory.NULL;
        }
        else if(isNormal()) {
            res = RedisMessageFactory.ERR_EXEC_MUL;
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
        if(isMulti()) return RedisMessageFactory.ERR_MULTI;
        setMulti();
        return RedisMessageFactory.OK;
    }
    public Object discard() {
        if(!isMulti()) return RedisMessageFactory.ERR_DISCARD;
        reset();
        return RedisMessageFactory.OK;
    }
    public void reset() {
        state &= 8;
        commands.clear();
        Iterator<String> iterator = watchedKeys.iterator();
        while(iterator.hasNext()) {
            String key = iterator.next();
            db.watchRemove(this, key);
            iterator.remove();
        }
    }

    public void blocked(long timeout, RedisObject target, String ...keys) {
        if(keys.length==0) return;
        unblocked();
        setBlocked();
        this.timeout = timeout==0? timeout: System.currentTimeMillis() + timeout;
        this.target = target;
        for(String key:keys) {
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
        Iterator<String> iterator = blockedKeys.iterator();
        if(!checkDb) {
            blockedKeys.clear();
            return;
        }
        while (iterator.hasNext()) {
            db.blockedRemove(this, iterator.next());
            iterator.remove();
        }
    }
    public void blockedExec(String key) {
//        unblocked(false);
//        RedisObject val = db.getRedisCommand().lPop(key);
//        if(target!=null) {
//            try {
//                db.getRedisCommand().lPush(target, val);
//            }catch (RedisException e) {
//                writeAndFlush(e.redisMessage);
//                // push失败 target不是list类型，push回去
//                db.getRedisCommand().lPush(key, val);
//                return;
//            }
//        }
//        writeAndFlush(new Object[]{key, val});
    }
    public void writeAndFlush(Object msg) {
        if (msg==null) {
            msg = RedisMessageFactory.NULL;
        }
        ctx.writeAndFlush(msg);
    }
    public void close() {
        reset();
        unblocked();
    }
}
