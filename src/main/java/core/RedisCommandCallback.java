package core;

public interface RedisCommandCallback{
    boolean support(RedisCommand cmd);
    void call(RedisCommand cmd, String[] args);
}
