package xyz.chenjm.redis.core;

import xyz.chenjm.redis.core.structure.RedisDict;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RedisDB {

    private final Map<String, Object> dict;
    private final Map<String, Long> expires;

    // 因watch而被监视的键
    private RedisDict<String, List<RedisClient>> watchedKeys;
    // 被阻塞的键
    private RedisDict<String, List<RedisClient>> blockedKeys;

    // 修改次数
    private int modCount;

    public RedisDB() {
        this.dict = new HashMap<>();
        this.expires = new HashMap<>();
    }

    /**
     * 检测被监视的键是否改动，在set、del等改变键值的方法中需要调用
     * @param key 修改的键
     */
    public void touchWatch(String key) {
        modCount ++;
        List<RedisClient> redisClients = watchedKeys.get(key);
        if(redisClients ==null) return;
        for(RedisClient client: redisClients) {
            client.setDirty();
        }
    }

    /**
     * 添加监视key,和客户端相关联
     *  这里没有验证key是否存在，官方就是这么实现的,可能是考虑到不存在的键被监视时可以监控它的添加
     *  没有去重措施，可以重复添加client，当然建议某个客户端不要重复watch某个键
     */
    public void watchAdd(RedisClient client, String key) {
        if(!watchedKeys.containsKey(key)){
            watchedKeys.put(key, new LinkedList<>());
        }
        watchedKeys.get(key).add(client);
    }

    /**
     * 取消某个客户端对某个key的监控
     */
    public void watchRemove(RedisClient client, String key) {
        List<RedisClient> clients = watchedKeys.get(key);
        if(clients==null) return;
        clients.remove(client);
        if(clients.isEmpty()){
            watchedKeys.remove(key);
        }
    }

    public void blockedAdd(RedisClient client, String key) {
        if(!blockedKeys.containsKey(key)){
            blockedKeys.put(key, new LinkedList<>());
        }
        blockedKeys.get(key).add(client);
    }
    public void blockedRemove(RedisClient client, String key) {
        List<RedisClient> clients = blockedKeys.get(key);
        if(clients==null) return;
        clients.remove(client);
        if(clients.isEmpty()){
            blockedKeys.remove(key);
        }
    }

    public void deleteKey(String key) {
        Object remove = dict.remove(key);
        if (remove != null) touchWatch(key);
    }

    public Object getAndDelete(String key) {
        Object value = dict.get(key);
        if(value == null)
            return null;
        long exp = expires.getOrDefault(key, Long.MAX_VALUE);
        if (exp < System.currentTimeMillis()) {
            dict.remove(key);
            touchWatch(key);
            expires.remove(key);
            value = null;
        }
        return value;
    }

    /**
     * 添加一个键值对,注意此时会重置过期时间并且总是成功
     */
    public void set(String key, Object value) {
        dict.put(key, value);
        expires.remove(key);
        touchWatch(key);
    }

    /**
     * 重新设置一个键值对,在对一些结构编码类型修改时需要用到，此时过期时间不应该被改变
     */
    public void reset(String key, Object value) {
        dict.put(key, value);
        touchWatch(key);
    }

    /**
     * 在某一时刻过期,只有key存在时才设置
     * @param mills 毫秒
     * @param check 是否检查key存在
     * @return 操作成功返回true，否则不存在key 返回false
     */
    public boolean expireAt(String key, long mills, boolean check) {
        if (check && getAndDelete(key)==null)
            return false;
        expires.put(key, mills);
        return true;
    }

    /**
     * 获取过期时间
     * @return 不存在key返回-2，没有过期时间则返回-1，否则返回在未来过期的ms
     */
    public long getTtl(String key) {
        Object o = dict.get(key);
        if (o == null)
            return -2;
        long l = expires.getOrDefault(key, -1L);
        if (l < 0)
            return -1;
        l -= System.currentTimeMillis();
        if (l > 0)
            return l;
        expires.remove(key);
        dict.remove(key);
        touchWatch(key);
        return -2;
    }


    void flushDb() {
        // 清空数据库，同时关联被监视的键
        for(String key:dict.keySet()) {
            touchWatch(key);
        }
        dict.clear();
        expires.clear();
    }


}
