package core;

import core.structure.RedisDict;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultRedisCommandHolder implements RedisCommandHolder {
    private final Map<String, RedisCommand> commandMap = new RedisDict<>();
    List<RedisCommandAround> around = new ArrayList<>();
    @Override
    public RedisCommand getCommand(String key) {
        return commandMap.get(key);
    }

    public void addCommand(String key, RedisCommand command) {
        commandMap.put(key, command);
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
