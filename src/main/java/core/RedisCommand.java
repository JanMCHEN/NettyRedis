package core;

public interface RedisCommand<T> {
    default boolean isMultiProcess() {
        return false;
    }

    /*
    return
    0:passed,
    -1:wrong number,
    -2:syntax error,
    -3:value not int,
    -4:
     */
    int checkArgs(byte[] ...args);
    T invoke(RedisClient client, byte[] ...args);
}
