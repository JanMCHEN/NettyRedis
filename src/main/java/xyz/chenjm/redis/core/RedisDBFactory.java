package xyz.chenjm.redis.core;

import java.util.Objects;

public class RedisDBFactory {
    private RedisDB[] dbs;
    private RedisDBFactory() {}

    public static RedisDBFactory build(int dbNum) {
        RedisDBFactory dbFactory = new RedisDBFactory();
        RedisDB[] dbs = new RedisDB[dbNum];
        for (int i=0;i<dbNum;++i) {
            dbs[i] = new RedisDB();
        }
        dbFactory.dbs = dbs;
        return dbFactory;
    }

    public RedisDB getDB(int idx) {
        Objects.checkIndex(idx, dbs.length);
        return dbs[idx];
    }
}
