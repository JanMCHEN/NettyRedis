package core;

import utils.Utils;

import java.io.*;
import java.util.*;

public class RedisDB implements Serializable {

    private static final long serialVersionUID = 3624988181265L;

    private final static RedisDB [] dbs = new RedisDB[16];
    static {
        // 初始化
        try {
            init();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("fail");
            for(int i=0;i<dbs.length;++i) {
                dbs[i] = new RedisDB();
            }
        }
    }

    public static RedisDB getDB(int i) {
        if(i>=dbs.length||i<0) {
            i = 0;
        }
        return dbs[i];
    }

    public static void removeFromExpire(int count) {
        for(var db:dbs) {
            db.checkExpire(count);
        }
    }

    private static void saveToFile() throws IOException {
        // 生成一个随机文件名后缀用来替换原文件
        String ext = String.valueOf(System.currentTimeMillis());
        File fileName =  new File("0.rdb."+ext);
        FileOutputStream file = new FileOutputStream(fileName);
        try (file; ObjectOutputStream oos = new ObjectOutputStream(file)) {
            modCount = 0;
            for (var db : dbs) {
                oos.writeObject(db);
            }
        }
        File tmp = new File(ext);
        File db = new File("0.rdb");
        boolean b = db.renameTo(tmp);
        if(b) {
            boolean b1 = fileName.renameTo(db);
            if(b1) {
                tmp.delete();
                return;
            }

        }
        // 替换失败，改回来
        tmp.renameTo(db);
        throw new IOException("rename fail");
    }

    public static void save(long modLimit) throws IOException {
        if(modLimit<=0 || modLimit > modCount) return;
        saveToFile();
    }

    public static void saveLast() throws IOException {
        removeFromExpire(0);
        saveToFile();
    }

    public static void init() throws IOException, ClassNotFoundException {
        FileInputStream file = new FileInputStream("0.rdb");
        try (file; ObjectInputStream ois = new ObjectInputStream(file)) {
            for (int i = 0; i < dbs.length; ++i) {
                dbs[i] = (RedisDB) ois.readObject();
                if(dbs[i] != null) dbs[i].watched_keys = new HashMap<>();
            }
        } catch (EOFException ignored) {
        }
    }

    private final HashMap<RedisObject, RedisObject> dict;
    private final HashMap<RedisObject, Long> expires;

    // 不参与序列化
    private transient HashMap<RedisObject, List<RedisClient>> watched_keys;
    private transient RedisCommand redisCommand = new RedisCommand();
    // 自上次保存修改次数
    private static transient volatile long modCount;

    public RedisCommand getRedisCommand() {
        // 序列化时忽略redisCommand,所以反序列化时值null,需要重新赋值
        // 有一点点多线程风险,多个线程获取同一个数据库的redisCommand时有风险,故加锁
        if(redisCommand==null) {
            synchronized (this) {
                if(redisCommand==null) {
                    redisCommand = new RedisCommand();
                }
            }
        }
        return redisCommand;
    }

    public void checkExpire(int count) {
        // 检查过期的键，一次只检查少量几个，防止工作线程阻塞过久
        Iterator<Map.Entry<RedisObject, Long>> iterator = expires.entrySet().iterator();
        while(iterator.hasNext() && count != 0 ) {
            RedisObject key = iterator.next().getKey();
            if(!checkKey(key)) {
                iterator.remove();
                deleteKey(key);
            }
            count--;
        }
    }

    private RedisDB() {
        dict = new HashMap<>();
        expires = new HashMap<>();
        watched_keys = new HashMap<>();
    }

    public void watch_add(RedisClient client, RedisObject key) {
        // 添加监视key,和客户端相关联
        // 这里没有验证key是否存在，官方就是这么实现的,可能是考虑到不存在的键被监视时可以监控它的添加
        //
        if(!watched_keys.containsKey(key)){
            watched_keys.put(key, new LinkedList<>());
        }
        watched_keys.get(key).add(client);
    }

    public void watch_remove(RedisClient client, RedisObject key) {
        watched_keys.get(key).remove(client);
    }

    public void touchWatch(RedisObject key) {
        // 检测被监视的键是否改动，在set、del等改变键值的方法中需要调用
        modCount ++;
        List<RedisClient> redisClients = watched_keys.get(key);
        if(redisClients ==null) return;
        for(var client: redisClients) {
            client.setDirty();
        }
    }

    public HashMap<RedisObject, RedisObject> getDict() {
        return dict;
    }

    public Set<RedisObject> getKeys() {
        return dict.keySet();
    }

    public void deleteKey(RedisObject key) {
        touchWatch(key);
        dict.remove(key);
    }

    public boolean checkKey(RedisObject key) {
        if(!dict.containsKey(key)) return false;
        if(!expires.containsKey(key)) return true;
        Long exp = expires.get(key);
        return exp > System.currentTimeMillis();
    }

    public boolean checkAndDelKey(RedisObject key) {
        if(!dict.containsKey(key)) return false;
        if(!expires.containsKey(key)) return true;
        Long exp = expires.get(key);
        if(exp > System.currentTimeMillis()) {
            return true;
        }
        expires.remove(key);
        deleteKey(key);
        return false;
    }

    void removeNull(RedisObject key) {
        RedisObject object = dict.get(key);
        if(object==null) return;
        if(object.isList() && ((RedisList)object.getPtr()).isEmpty()){
            deleteKey(key);
        }
        else if(object.isSet() && ((RedisSet)object.getPtr()).isEmpty()) {
            deleteKey(key);
        }
    }
    private void removeNull(RedisObject key, RedisSet value){
        if(value.isEmpty()){
            deleteKey(key);
        }
    }
    private void removeNull(RedisObject key, RedisList value){
        if(value.isEmpty()){
            deleteKey(key);
        }
    }

    /**
     * 没定义成static主要考虑到里边操作和当前数据库有关
     * Redis支持的操作全部在这里定义，主要参考Redis官方规范
     */
    public class RedisCommand {

        private RedisCommand(){}

        public boolean save() {
            try {
                RedisDB.save(0);
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        public List<RedisObject> keys(String pattern) {
            Set<RedisObject> keys = getKeys();
            ArrayList<RedisObject> ans = new ArrayList<>();
            for(var key:keys) {
                if(checkKey(key) && Utils.match((String) key.getPtr(), pattern)) {
                    ans.add(key);
                }
            }
            return ans;
        }
        public boolean exists(RedisObject key) {
            return checkAndDelKey(key);
        }
        public boolean expire(RedisObject key, long value) {
            if(checkAndDelKey(key)) {
                expires.put(key, value*1000+System.currentTimeMillis());
                return true;
            }
            return false;
        }
        public boolean expireAt(RedisObject key, long time) {
            if(checkAndDelKey(key)) {
                if (time > System.currentTimeMillis()) {
                    expires.put(key, time);
                }
                return true;
            }
            return false;
        }
        public Long ttl(RedisObject key) {
            if(!dict.containsKey(key)) return -2L;
            if(!expires.containsKey(key)) return -1L;
            long ans = expires.get(key) - System.currentTimeMillis();
            return ans>0? ans/1000:-2L;
        }
        public int type(RedisObject key) {
            if(checkAndDelKey(key)) {
                return dict.get(key).getType();
            }
            return -1;
        }
        public int del(RedisObject ...keys) {
            int ans = 0;
            for(var key:keys) {
                if(checkAndDelKey(key)) ans++;
            }
            return ans;
        }
        public boolean persist(RedisObject key) {
            if(checkKey(key)) {
                expires.remove(key);
                return true;
            }
            return false;
        }

        public RedisObject get(RedisObject key) {
            checkAndDelKey(key);
            return dict.get(key);
        }
        public boolean set(RedisObject key, RedisObject value, Long px, boolean nx, boolean xx) {
            if(nx || xx) {
                boolean check = checkKey(key);
                if(check && xx) {
                    return set(key, value, px, false, false);
                }
                else if(!check && nx) {
                    return set(key, value, px, false, false);
                }
                return false;
            }
            touchWatch(key);
            dict.put(key, value);
            if (px != null) {
                expires.put(key, px+System.currentTimeMillis());
            }
            return true;
        }

        public long sAdd(RedisObject key, boolean isInt, RedisObject ...values) {
            RedisObject obj = get(key);
            if(obj==null) {
                // 创建新的IntSet对象
                obj = RedisObject.newSet();
                dict.put(key, obj);
            }
            if(!obj.isSet()) {
                return -1;
            }
            RedisSet ptr = (RedisSet) obj.getPtr();
            if(obj.getEncoding()==RedisObject.REDIS_ENCODING_INTSET) {
                if(isInt) {
                    long ans = ptr.add(values);
                    if(ans>=0) return ans;
                    // 否则超出IntSet插入范围 st<0
                }
                ptr = ((IntSet)ptr).upToHash();
                obj = RedisObject.newSet(ptr, false);
                dict.put(key, obj);
            }
            return ptr.add(values);
        }
        public long sRemove(RedisObject key, RedisObject ...values) {
            RedisObject obj = get(key);
            if(obj==null) {
                return 0;
            }
            if(obj.getType()!=RedisObject.OBJ_SET) {
                return -1;
            }
            long ans = ((RedisSet) obj.getPtr()).remove(values);
            removeNull(key, (RedisSet) obj.getPtr());
            return ans;

        }
        public long sContain(RedisObject key, RedisObject member) {
            RedisObject obj = get(key);
            if(obj == null) return 0;
            if(obj.getType()!=RedisObject.OBJ_SET) return -1;
            return ((RedisSet) obj.getPtr()).contains(member)?1:0;
        }
        public Object sMembers(RedisObject key) {
            RedisObject obj = get(key);
            if(obj == null) return Collections.EMPTY_LIST;
            if(obj.getType()!=RedisObject.OBJ_SET) return null;
            return ((RedisSet) obj.getPtr()).members();
        }
        public long sCard(RedisObject key) {
            RedisObject obj = get(key);
            if(obj==null) return 0;
            if(obj.getType()!=RedisObject.OBJ_SET) return -1;
            return ((RedisSet)obj.getPtr()).size();
        }

        public RedisObject lPop(RedisObject key) {
            RedisObject obj = get(key);
            if(obj==null) {
                return null;
            }
            RedisObject ans;
            if(obj.isList()){
                RedisList ptr = (RedisList) obj.getPtr();
                ans = ptr.popFirst();
                removeNull(key, ptr);
            }
            else{
                ans = RedisObject.ERROR;
            }
            return ans;
        }
        public RedisObject rPop(RedisObject key) {
            RedisObject obj = get(key);
            if(obj==null) {
                return null;
            }
            RedisObject ans;
            if(obj.isList()){
                RedisList ptr = (RedisList) obj.getPtr();
                ans = ptr.popLast();
                removeNull(key, ptr);
            }
            else{
                ans = RedisObject.ERROR;
            }
            return ans;
        }
        public long lPush(RedisObject key, RedisObject value){
            RedisObject obj = get(key);
            RedisList ptr;
            if(obj == null) {
                ptr = new RedisList();
                obj = RedisObject.newList(ptr, false);
                dict.put(key, obj);
            }
            else if(obj.isList()) {
                ptr = (RedisList) obj.getPtr();
            }
            else return -1;
            ptr.addFirst(value);
            return ptr.size();
        }
        public long rPush(RedisObject key, RedisObject value){
            RedisObject obj = get(key);
            RedisList ptr;
            if(obj == null) {
                ptr = new RedisList();
                obj = RedisObject.newList(ptr, false);
                dict.put(key, obj);
            }
            else if(obj.isList()) {
                ptr = (RedisList) obj.getPtr();
            }
            else return -1;
            ptr.addLast(value);
            return ptr.size();
        }

    }

    public static void main(String[] args) throws IOException {
        System.out.println(dbs[0].dict.toString());
        save(0);
    }

}
