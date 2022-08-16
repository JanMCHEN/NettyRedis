package core;

public interface RedisCommandAround {

    default void before(RedisClient client, String ...args) {
    }
    default Object after(Object returnValue, RedisClient client, String ...args) {
        return returnValue;
    }
}
