package xyz.chenjm.redis.command;

import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.command.RedisCommandAround;
import xyz.chenjm.redis.command.RedisCommandHolder;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.structure.RedisDict;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultRedisCommandHolder implements RedisCommandHolder {


    private final Map<String, RedisCommand> commandMap = new RedisDict<>();
    List<RedisCommandAround> around = new ArrayList<>();
    @Override
    public RedisCommand getCommand(String key) {
        return commandMap.get(key.toUpperCase());
    }

    public void addCommand(String key, RedisCommand command) {
        Class<? extends RedisCommand> cls = command.getClass();
        commandMap.put(key.toUpperCase(), command);
    }

    @Override
    public void before(RedisClient client, String... args) {
        if(!around.isEmpty()) {
            around.forEach(a->a.before(client, args));
        }
    }

    @Override
    public Object after(Object returnValue, RedisClient client, String... args) {
        int n = around.size();
        if(n==0) return returnValue;
        for(int i=n-1;i>=0;--i) {
            returnValue = around.get(i).after(returnValue, client, args);
        }
        return returnValue;
    }

    public void addAround(RedisCommandAround rca) {
        if(rca != null) around.add(rca);
    }
}
