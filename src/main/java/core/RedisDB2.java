package core;

import core.exception.RedisException;
import core.structure.*;
import core.structure.SortedSet;
import utils.CommonUtils;

import java.io.*;
import java.util.*;

@SuppressWarnings("all")
public class RedisDB2 implements Serializable {

    private static final long serialVersionUID = 3624988181265L;

    public static RedisDB2 [] dbs;

    public static RedisDB2 getDB(int i) {
        if(i>=dbs.length||i<0) {
            i = 0;
        }
        return dbs[i];
    }
    static void flushAll() {
        for(RedisDB2 db:dbs) {
            db.flushDb();
        }
    }

    public static void removeFromExpire(int count) {
        for(RedisDB2 db:dbs) {
            db.checkExpire(count);
        }
    }

    private static void save() throws IOException {
        // 生成一个随机文件名后缀用来替换原文件
        String ext = String.valueOf(System.currentTimeMillis());
        File fileName =  new File("0.rdb."+ext);
        FileOutputStream file = new FileOutputStream(fileName);
        try (file; ObjectOutputStream oos = new ObjectOutputStream(file)) {
            modCount = 0;
            for (RedisDB2 db : dbs) {
                oos.writeObject(db);
            }
        }
        File tmp = new File(ext);
        File db = new File("0.rdb");
        if(!db.exists()) {
            // db文件不存在时直接重命名
            fileName.renameTo(db);
            return;
        }
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

    public static void saveLast() throws IOException {
        removeFromExpire(0);
        save();
    }

    public static void init(int dbCnt) throws IOException, ClassNotFoundException {
        dbs = new RedisDB2[dbCnt];
        try (FileInputStream file = new FileInputStream("0.rdb"); ObjectInputStream ois = new ObjectInputStream(file)) {
            for (int i = 0; i < dbs.length; ++i) {
                dbs[i] = (RedisDB2) ois.readObject();
                if (dbs[i] != null) {
                    dbs[i].watchedKeys = new RedisDict<>();
                    dbs[i].blockedKeys = new RedisDict<>();
                }
            }
        } catch (FileNotFoundException e) {
            for (int i = 0; i < dbs.length; ++i) {
                dbs[i] = new RedisDB2();
            }
        }

    }

    private final RedisDict<String, RedisObject> dict;
    private final RedisDict<String, Long> expires;

    // 不参与序列化
    private transient RedisDict<String, List<RedisClient>> watchedKeys;
    private transient RedisDict<String, List<RedisClient>> blockedKeys;
    private transient volatile RedisCommand redisCommand = new RedisCommand();
    // 自上次保存修改次数
    private static long modCount;
    // 当前expire检查的个数，保证能扫描到整个expire字典

    // 上次保存时间
    private static long saveTime = System.currentTimeMillis();

    public static Runnable saveTask = () -> {
        try {
            RedisDB2.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public static boolean isSaveNeed(long delay, long count) {
        if(modCount >= count && System.currentTimeMillis()-saveTime>=delay*1000) {
            modCount = 0;
            saveTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

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
        while (count > 0) {
            String key = expires.getRandom().getKey();
            if (!checkKey(key)) {
                expires.remove(key);
                deleteKey(key);
            }
            count--;
        }
    }

    private RedisDB2() {
        dict = new RedisDict<>();
        expires = new RedisDict<>();
        watchedKeys = new RedisDict<>();
        blockedKeys = new RedisDict<>();
    }

    void flushDb() {
        // 清空数据库，同时关联被监视的键
        for(String key:dict.keySet()) {
            touchWatch(key);
        }
        dict.clear();
        expires.clear();

    }

    public void watchAdd(RedisClient client, String key) {
        // 添加监视key,和客户端相关联
        // 这里没有验证key是否存在，官方就是这么实现的,可能是考虑到不存在的键被监视时可以监控它的添加
        // 没有去重措施，可以重复添加client，当然建议某个客户端不要重复watch某个键
        if(!watchedKeys.containsKey(key)){
            watchedKeys.put(key, new LinkedList<>());
        }
        watchedKeys.get(key).add(client);
    }
    public void watchRemove(RedisClient client, RedisObject key) {
        List<RedisClient> clients = watchedKeys.get(key);
        if(clients==null) return;
        clients.remove(client);
        if(clients.isEmpty()){
            watchedKeys.remove(key);
        }
    }
    public void touchWatch(String key) {
        // 检测被监视的键是否改动，在set、del等改变键值的方法中需要调用
        modCount ++;
        List<RedisClient> redisClients = watchedKeys.get(key);
        if(redisClients ==null) return;
        for(RedisClient client: redisClients) {
            client.setDirty();
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
    public static void checkBlockedTimeout() {
        for(RedisDB2 db:dbs) {
            for (String key : db.blockedKeys.keySet()) {
                db.checkBlockedTimeout(key);
            }
        }
    }
    void checkBlockedTimeout(String key) {
        List<RedisClient> list = blockedKeys.get(key);
        if(list==null) return;
        Iterator<RedisClient> iterator = list.iterator();
        RedisClient client;
        while (iterator.hasNext()){
            client = iterator.next();
            if(client.timeout==0 || !CommonUtils.isExpire(client.timeout)){
                continue;
            }
            iterator.remove();
            client.unblocked(false);
            client.writeAndFlush(null);
        }
    }
    void touchBlocked(RedisObject key) {
        List<RedisClient> list = blockedKeys.get(key);
        if(list==null) return;
        Iterator<RedisClient> iterator = list.iterator();
        RedisClient client;
        while (iterator.hasNext()){
            client = iterator.next();
            iterator.remove();
            if(client.timeout==0 || !CommonUtils.isExpire(client.timeout)){
                client.blockedExec(key);
                break;
            }
            client.unblocked(false);
        }

    }

    public RedisDict<String, RedisObject> getDict() {
        return dict;
    }

    public Set<String> getKeys() {
        return dict.keySet();
    }

    public void deleteKey(String key) {
        touchWatch(key);
        dict.remove(key);
    }

    public boolean checkKey(String key) {
        if(!dict.containsKey(key)) return false;
        if(!expires.containsKey(key)) return true;
        Long exp = expires.get(key);
        return exp > System.currentTimeMillis();
    }

    public boolean checkAndDelKey(String key) {
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

    void removeNull(String key) {
        RedisObject object = dict.get(key);
        if(object==null) return;
        if(object instanceof RedisList && ((RedisList) object).isEmpty()){
            deleteKey(key);
        }
        else if(object instanceof RedisSet && ((RedisSet) object).isEmpty()) {
            deleteKey(key);
        }
    }
    private void removeNull(String key, RedisSet value){
        if(value.isEmpty()){
            deleteKey(key);
        }
    }
    private void removeNull(String key, RedisList value){
        if(value.isEmpty()){
            deleteKey(key);
        }
    }

    public RedisObject get(String key) {
        checkAndDelKey(key);
        return dict.get(key);
    }

    public RedisString getString(String key) {
        RedisObject obj = get(key);
        if(obj ==null || obj instanceof RedisString){
            return (RedisString) obj;
        }
        throw RedisException.ERROR_TYPE;
    }

    public RedisList getList(String key) {
        RedisObject obj = get(key);
        if(obj==null) return null;
        if(obj instanceof RedisList) return (RedisList) obj;
        throw RedisException.ERROR_TYPE;
    }

    public RedisSet getSet(String key){
        RedisObject obj = get(key);
        if(obj==null) return null;
        if(obj instanceof RedisSet) return (RedisSet) obj;
        throw RedisException.ERROR_TYPE;
    }
    public RedisDict<String, RedisString> getHash(String key) {
        RedisObject obj = get(key);
        if(obj==null) return null;
        if(obj instanceof RedisDict) return (RedisDict<String, RedisString>) obj;
        throw RedisException.ERROR_TYPE;
    }




    /**
     * 数据库相关辅助命令
     */
    public class RedisCommand {

        private RedisCommand(){}

        public Object[] keys(String pattern) {
            Set<RedisObject> keys = getKeys();
            List<RedisObject> ans = new LinkedList<>();
            for(RedisObject key:keys) {
                if(checkKey(key) && CommonUtils.match(key.toString(), pattern)) {
                    ans.add(key);
                }
            }
            return ans.toArray();
        }
        public long exists(RedisObject ...keys) {
            long ans = 0;
            for(RedisObject key:keys){
                if(checkAndDelKey(key)) ans++;
            }
            return ans;
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
        public long ttl(RedisObject key) {
            if(!dict.containsKey(key)) return -2L;
            if(!expires.containsKey(key)) return -1L;
            long ans = expires.get(key) - System.currentTimeMillis();
            return ans>0? ans/1000:-2L;
        }
        public int type(RedisObject key) {
            return RedisObject.getType(get(key));
        }
        public long del(RedisObject ...keys) {
            int ans = 0;
            for(RedisObject key:keys) {
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
        public RedisString getString(RedisObject key) {
            RedisObject obj = get(key);
            if(obj ==null || obj instanceof RedisString){
                return (RedisString) obj;
            }
            throw RedisException.ERROR_TYPE;
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
        public long strLen(RedisObject key) {
            RedisString obj = getString(key);
            if(obj==null) {
                return 0;
            }
            return obj.size();
        }
        public long getBit(RedisObject key, long offset) {
            RedisString string = getString(key);
            if(string==null) return 0;
            return string.getBit(offset);
        }
        public long setBit(RedisObject key, long offset, int bit) {
            long ans = 0;
            RedisString string = getString(key);
            if(string==null) {
                string = new RedisString.RawString(new byte[(int) (offset >> 3)+1]);
                dict.put(key, string);
            }
            else if(!(string instanceof RedisString.RawString)) {
                string = RedisString.toRaw(string);
                dict.put(key, string);
            }
            ans = string.getBit(offset);
            touchWatch(key);
            string.setBit(offset, bit);
            return ans;
        }
        public long bitCount(RedisObject key) {
            RedisString string = getString(key);
            if(string==null) return 0;
            return string.bitCount();
        }
        public long increase(RedisObject key, long v) {
            RedisString string = getString(key);
            long before;
            try{
                before = string==null? 0:string.get();
            }catch (NumberFormatException e){
                throw RedisException.ERROR_INT;
            }
            if(v == 0) return before;
            touchWatch(key);

            if(!(string instanceof RedisString.IntString)) {
                string = new RedisString.IntString(before+v);
                dict.put(key, string);
            }
            else {
                string.increase(v);
            }
            return before + v;
        }
        public long append(RedisObject key, byte[] values) {
            RedisString string = getString(key);
            if(string==null) {
                string = new RedisString.RawString(values);
                dict.put(key, string);
                return values.length;
            }
            if(!(string instanceof RedisString.RawString)) {
                string = new RedisString.RawString(string);
                dict.put(key, string);
            }
            return ((RedisString.RawString)string).append(values);
        }


        private RedisSet getSet(RedisObject key){
            RedisObject obj = get(key);
            if(obj==null) return null;
            if(obj instanceof RedisSet) return (RedisSet) obj;
            throw RedisException.ERROR_TYPE;
        }
        public long sAdd(RedisObject key, boolean isInt, RedisObject ...values) {
            RedisSet set = getSet(key);
            if(set==null) {
                // 创建新的IntSet对象
                set = new IntSet();
                dict.put(key, set);
            }
            if(set instanceof IntSet) {
                if(isInt) {
                    // IntSet的插入
                    long ans = set.add(values);
                    if (ans >= 0) return ans;
                }
                // 插入失败，ans==-1,IntSet达到最大长度，或者待插入数据不是int，isInt==false
                // 转为hash
                set = ((IntSet) set).upToHash();
                dict.put(key, set);
            }
            return set.add(values);
        }
        public long sRemove(RedisObject key, RedisObject ...values) {
            RedisSet set = getSet(key);
            if(set==null) {
                return 0;
            }
            long ans = set.remove(values);
            removeNull(key, set);
            return ans;

        }
        public long sContain(RedisObject key, RedisObject member) {
            RedisSet set = getSet(key);
            if(set == null) return 0;
            return set.contains(member)?1:0;
        }
        public Object[] sMembers(RedisObject key) {
            RedisSet set = getSet(key);
            if(set == null) return new Object[0];
            return set.members().toArray();
        }
        public long sCard(RedisObject key) {
            RedisSet set = getSet(key);
            if(set==null) return 0;
            return set.size();
        }

        private RedisList getList(RedisObject key) {
            RedisObject obj = get(key);
            if(obj==null) return null;
            if(obj instanceof RedisList) return (RedisList) obj;
            throw RedisException.ERROR_TYPE;
        }
        public RedisObject lPop(RedisObject key) {
            RedisList list = getList(key);
            if(list==null) {
                return null;
            }
            touchWatch(key);
            RedisObject ans = list.popFirst();
            removeNull(key, list);
            return ans;
        }
        public RedisObject rPop(RedisObject key) {
            RedisList list = getList(key);
            if(list==null) {
                return null;
            }
            touchWatch(key);
            RedisObject ans = list.popLast();
            removeNull(key, list);
            return ans;
        }
        public long lPush(RedisObject key, RedisObject ...values){
            RedisList list = getList(key);
            if(list==null) {
                list = new RedisList();
                dict.put(key, list);
            }
            for(RedisObject value:values) {
                list.addFirst(value);
                touchBlocked(key);
            }
            touchWatch(key);
            return list.size();
        }
        public long rPush(RedisObject key, RedisObject ...values){
            RedisList list = getList(key);
            if(list==null) {
                list = new RedisList();
                dict.put(key, list);
            }
            for(RedisObject value:values) {
                list.addLast(value);
                touchBlocked(key);
            }
            touchWatch(key);
            return list.size();
        }
        public long lPushX(RedisObject key, RedisObject value){
            RedisList list = getList(key);
            if(list==null) {
                return 0;
            }
            touchWatch(key);
            list.addFirst(value);
            touchBlocked(key);
            return list.size();
        }
        public long rPushX(RedisObject key, RedisObject value){
            RedisList list = getList(key);
            if(list==null) {
                return 0;
            }
            touchWatch(key);
            list.addLast(value);
            touchBlocked(key);
            return list.size();
        }
        public long lLen(RedisObject key){
            RedisList list = getList(key);
            if(list==null) {
                return 0;
            }
            return list.size();
        }
        public RedisObject rPopLPush(RedisObject from, RedisObject to){
            RedisObject ans = rPop(from);
            if(ans==null){
                return null;
            }
            lPush(to, ans);
            return ans;
        }
        public Object[] lRange(RedisObject key, long start, long stop) {
            RedisList list = getList(key);
            if(list==null){
                return new Object[0];
            }
            return list.getRange(start, stop);
        }
        public Object[] bLPop(RedisObject ...keys) {
            RedisObject ans;
            for(RedisObject key:keys) {
                ans = lPop(key);
                if(ans!=null){
                    return new Object[]{key, ans};
                }
            }
            return null;
        }
        public Object[] bRPop(RedisObject ...keys) {
            RedisObject ans;
            for(RedisObject key:keys) {
                ans = rPop(key);
                if(ans!=null){
                    return new Object[]{key, ans};
                }
            }
            return null;
        }
        public Object bRPopLPush(RedisObject from, RedisObject to) {
            Object[] o = bRPop(from);
            if(o==null) {
                return null;
            }
            lPush(to, (RedisObject) o[1]);
            return o[1];
        }

        // zset
        private core.structure.SortedSet getZSet(RedisObject key){
            RedisObject obj = get(key);
            if(obj==null) return null;
            if(obj instanceof core.structure.SortedSet) return (core.structure.SortedSet) obj;
            throw RedisException.ERROR_TYPE;
        }

        public long zadd(RedisObject key, int score, RedisObject member) {
            core.structure.SortedSet zSet = getZSet(key);
            if(zSet==null) {
                zSet = new SortedSet();
                dict.put(key, zSet);
            }
            zSet.add(member, score);
            return zSet.size();
        }
    }
}
