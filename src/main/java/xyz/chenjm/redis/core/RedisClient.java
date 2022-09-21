package xyz.chenjm.redis.core;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.command.Command2;
import xyz.chenjm.redis.command.CommandExecutor;
import xyz.chenjm.redis.exception.RedisException;
import xyz.chenjm.redis.core.structure.RedisObject;

import java.util.*;

/**
 * 客户端状态管理
 */
@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class RedisClient {
    static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    static public class CommandWithArgs{
        private final Command2 method;
        private final String[]args;

        public CommandWithArgs(Command2 method, String[] args) {
            this.method = method;
            this.args = args;
        }

        public Command2 getMethod() {
            return method;
        }

        public String[] getArgs() {
            return args;
        }
    }

    private volatile int state = 0;
    private final List<CommandWithArgs> commands = new ArrayList<>();
    private final HashSet<String> watchedKeys = new HashSet<>();
    private volatile int selectDb = 0;
    private volatile RedisDB db;
    private final SocketChannel channel;
    private final RedisServer server;

    private final CommandExecutor commandExecutor;

    // blocked
    long timeout;
    List<String> blockedKeys = new LinkedList<>();
    RedisObject target;


    public RedisClient(SocketChannel ch, RedisServer server) {
        channel = ch;
        this.server = server;
        db = server.getDB(0);
        commandExecutor = server.getCmdExecutor();
    }

    public RedisDB getDb() {
        return db;
    }

    public int selectDb() {
        return selectDb;
    }
    public void selectDb(int i) {
        db = server.getDB(i);
        selectDb = i;
    }

    public void execute(Runnable task) {
        server.getEventLoop().submit(task);
    }
    public void execute(String[] args) {
        Command2 cmd = commandExecutor.getCommand(args);

        if (isMulti() && cmd.multi()) {
            addCommand(cmd, args);
            writeAndFlush(RedisMessageFactory.QUEUED);
        }else {
            execute(()->{
                Object res;
                try{
                    res = commandExecutor.call(this, cmd, args);
                }catch (RedisException e) {
                    if (e.isError())
                        setError();
                    res = new ErrorRedisMessage(e.getMessage());
                }
                writeAndFlush(res);
            });
        }
    }

    /*       客户端状态管理              */
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
    /*      END              */


    public void addCommand(Command2 method , String[] args) {
        if(isError()||isDirty()) {
            commands.clear();
            return;
        }
        commands.add(new CommandWithArgs(method, args));
    }

    public List<CommandWithArgs> getCommands() {
        return commands;
    }

    public Object watch(String...keys) {
        if(isMulti()) return RedisMessageFactory.ERR_WATCH;
        RedisDB db = getDb();
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
        if (!isMulti()) {
            return RedisMessageFactory.ERR_EXEC_MUL;
        }
        Object res;
        if(isError()) {
            res = RedisMessageFactory.ERR_EXEC_ERR;
        }else if(isDirty()) {
            res = RedisMessageFactory.NULL;
        } else {
            Object[] arr = new Object[commands.size()];
            for (int i=0;i<arr.length;++i) {
                Object ans;
                CommandWithArgs next = commands.get(i);
                try {
                    ans = commandExecutor.call(this, next.getMethod(), next.getArgs());
                } catch (RedisException e){
                    ans = new ErrorRedisMessage(e.getMessage());
                }catch (Exception e) {
                    log.error("事务中命令'{}'执行错误", next.getMethod().getName(), e);
                    ans = RedisMessageFactory.ERR;
                }
                arr[i] = ans;
            }
            res = arr;
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

    /**
     * 此操作涉及到db的操作，应该考虑执行时的线程安全问题
     */
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
        RedisDB db = getDb();
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
        channel.writeAndFlush(msg);
    }
    public void close() {
        reset();
        unblocked();
    }
}
