package xyz.chenjm.redis.core;

import io.netty.channel.EventLoop;
import xyz.chenjm.redis.command.CommandHolder;
import xyz.chenjm.redis.command.RedisCommand;

import java.util.concurrent.Callable;

public class RedisServer {
    EventLoop eventLoop;
    RedisDB[] dbs;

    CommandHolder commandHolder;
    EventPublisher<CommandTask> commandPublisher;
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

    public void setCommandPublisher(EventPublisher<CommandTask> commandPublisher) {
        this.commandPublisher = commandPublisher;
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
        eventLoop.execute(task);
    }

    public void execute(Callable<?> task) {
        eventLoop.submit(task);
    }

    public CommandTask newTask(RedisClient client, RedisCommand cmd, String[] args) {
        CommandTask task = new CommandTask(client, cmd, args);
        task.setPublisher(commandPublisher);
        return task;
    }

}
