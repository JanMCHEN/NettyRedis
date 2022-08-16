package core;

public interface RedisCommand{
    default boolean isMultiProcess() {
        return false;
    }

    /**
     * 参数检查
     * @param args 参数数组
     * @return  0:passed,
     *          -1:wrong number,
     *          -2:syntax error,
     *          -3:value not int,
     *          -4:
     */
    int checkArgs(String ...args);

    /**
     * 执行命令
     * @param client    状态
     * @param args      参数数组
     * @return object   结果
     */
    Object invoke(RedisClient client, String ...args);
}
