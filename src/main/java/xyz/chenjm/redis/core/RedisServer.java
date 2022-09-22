package xyz.chenjm.redis.core;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.util.concurrent.Future;
import xyz.chenjm.redis.command.CommandExecutor;
import xyz.chenjm.redis.command.CommandHolder;
import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.exception.RedisException;

public class RedisServer {
    EventLoop eventLoop;
    RedisDB[] dbs;

    CommandHolder commandHolder;
    ReplicateMaster master;

    long offset = 0;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public void setDbs(RedisDB[] dbs) {
        this.dbs = dbs;
    }

    public void setCommandHolder(CommandHolder commandHolder) {
        this.commandHolder = commandHolder;
    }

    static class ReplicateMaster {
        String masterHost;
        int masterPort;
        public ReplicateMaster(String host, int port) {
            masterHost = host;
            masterPort = port;
        }
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public RedisDB getDB(int index) {
        return dbs[index];
    }

    public void setSlave(String host, int port) {
        master = new ReplicateMaster(host, port);
    }

    public boolean isSlave() {
        return master != null;
    }

    public RedisCommand getCommand(String... args) {
        return commandHolder.getCommand(args);
    }

    public void execute(Runnable task) {
        if (task instanceof CommandTask) {

        }
        eventLoop.submit(task);
    }

}
