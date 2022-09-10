package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;

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
    default int checkArgs(String ...args) {
        return 0;
    }

    /**
     * 执行命令
     * @param client    状态
     * @param args      参数数组
     * @return object   结果
     */
    Object invoke(RedisClient client, String ...args);
}
