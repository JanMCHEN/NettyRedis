package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.Event;
import xyz.chenjm.redis.core.RedisClient;

public class CommandEvent implements Event {
    RedisClient client;
    RedisCommand cmd;
    Object res;
    String[] args;

    public CommandEvent(RedisClient client, RedisCommand cmd, Object res, String[] args) {
        this.client = client;
        this.cmd = cmd;
        this.res = res;
        this.args = args;
    }

    public RedisClient getClient() {
        return client;
    }

    public RedisCommand getCmd() {
        return cmd;
    }

    public Object getRes() {
        return res;
    }

    public String[] getArgs() {
        return args;
    }
}
