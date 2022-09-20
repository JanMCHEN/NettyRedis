package xyz.chenjm.redis.core;

import io.netty.channel.EventLoop;
import xyz.chenjm.redis.command.CommandExecutor;

public class RedisServer {
    EventLoop eventLoop;
    RedisDB[] dbs;

    CommandExecutor cmdExecutor;

    ReplicateMaster master;

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public void setDbs(RedisDB[] dbs) {
        this.dbs = dbs;
    }

    public CommandExecutor getCmdExecutor() {
        return cmdExecutor;
    }

    public void setCmdExecutor(CommandExecutor cmdExecutor) {
        this.cmdExecutor = cmdExecutor;
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

}
