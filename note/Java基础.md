## Java基础

### 多线程基础

#### synchronized

1. Java管程支持，只能锁对象，原理是对象头记录了一个锁有关的结构体ObjectMonitor，可重入，记录持有的线程，等待队列，和阻塞队列。当一个线程获取到锁时，记录持有线程，调用await时（一般作为条件变量使用，所以会有一个while循环），释放锁，插入等待队列。其它线程调用notify后，将原来阻塞在等待队列的线程移出，放入阻塞队列，直到下一次获取锁。

### Redis

#### 数据结构

##### object

5中基本类型的结构体，包括引用计数，近似的lru算法实现

```c
#define REDIS_STRING 0
#define REDIS_LIST 1
#define REDIS_SET 2
#define REDIS_ZSET 3
#define REDIS_HASH 4

#define REDIS_ENCODING_RAW 0     /* Raw representation */
#define REDIS_ENCODING_INT 1     /* Encoded as integer */
#define REDIS_ENCODING_HT 2      /* Encoded as hash table */
#define REDIS_ENCODING_ZIPMAP 3  /* Encoded as zipmap */
#define REDIS_ENCODING_LINKEDLIST 4 /* Encoded as regular linked list */
#define REDIS_ENCODING_ZIPLIST 5 /* Encoded as ziplist */
#define REDIS_ENCODING_INTSET 6  /* Encoded as intset */
#define REDIS_ENCODING_SKIPLIST 7  /* Encoded as skiplist */
#define REDIS_ENCODING_EMBSTR 8  /* Embedded sds string encoding */

#define REDIS_LRU_BITS 24       /* 记录lru时间的比特数 */
#define REDIS_LRU_CLOCK_MAX ((1<<REDIS_LRU_BITS)-1) /* Max value of obj->lru */
#define REDIS_LRU_CLOCK_RESOLUTION 1000 /* LRU clock resolution in ms */

typedef struct redisObject {

    unsigned type:4;			/* 可选值为上面的宏定义，代表redis支持的5种数据类型 */
    unsigned encoding:4;		/* 不同类型有不同编码类型 */
    
    unsigned lru:REDIS_LRU_BITS; /* lru time (relative to server.lruclock) */
    							/* REDIS_LRU_BITS = 24 近似lru算法实现 */

    int refcount;		// 引用计数
    void *ptr;			// 数据指针

} robj;
```



##### string

```c
/**
    len：数据长度，free：剩余的长度
    len+free:buf的总长度
*/
struct sdshdr {
    int len;
    int free;
    char buf[];
};

/* Create a string object with encoding REDIS_ENCODING_EMBSTR, that is
 * an object where the sds string is actually an unmodifiable string
 * allocated in the same chunk as the object itself. 
 * emb字符串，此时，内存是和robj一起分配的，更节省内存，连续的地址空间加载更快
 * 不可变，且有大小限制
 */
robj *createEmbeddedStringObject(char *ptr, size_t len) {
    robj *o = zmalloc(sizeof(robj)+sizeof(struct sdshdr)+len+1);
    struct sdshdr *sh = (void*)(o+1);

    o->type = REDIS_STRING;
    o->encoding = REDIS_ENCODING_EMBSTR;
    o->ptr = sh+1;
    o->refcount = 1;
    o->lru = LRU_CLOCK();

    sh->len = len;
    sh->free = 0;
    if (ptr) {
        memcpy(sh->buf,ptr,len);
        sh->buf[len] = '\0';		// len不包括'\0',这里主要为了兼容c的字符串
    } else {
        memset(sh->buf,0,len+1);
    }
    return o;
}
/* Create a string object with EMBSTR encoding if it is smaller than
 * REIDS_ENCODING_EMBSTR_SIZE_LIMIT, otherwise the RAW encoding is
 * used.
 *
 * The current limit of 39 is chosen so that the biggest string object
 * we allocate as EMBSTR will still fit into the 64 byte arena of jemalloc. */
#define REDIS_ENCODING_EMBSTR_SIZE_LIMIT 39		/* 上文中说到的大小限制，上面的英文解释
												 * sizeof(robj) = (4+4+24)/8+4+4+sds													 * sizeof(sds) = 4+4+4+buf
												 * all = 12 + 12 + limit(39) + '\0' = 64
												 * 后面版本有改进，emb单独有sds8，这样len和
												 * free都只需要一个字节，在加个char flag，共
												 * 3b，比原来的8b减少5b，所以限制为39+5=44
												*/
robj *createStringObject(char *ptr, size_t len) {
    if (len <= REDIS_ENCODING_EMBSTR_SIZE_LIMIT)
        return createEmbeddedStringObject(ptr,len);
    else
        return createRawStringObject(ptr,len);
}
/* long long相当于Java的long,如果在缓存范围内会直接使用缓存，在判断在int范围内直接将ptr转成long类型，否则需要用sds以数字形式存储 */
robj *createStringObjectFromLongLong(long long value);	
robj *createStringObjectFromLongDouble(long double value);
```



#### redis分布式锁

##### redission

![img](https://img2018.cnblogs.com/blog/1090617/201906/1090617-20190618183025891-1248337684.jpg)

###### 加锁

加锁是对hash数据结构进行操作，一般用lua脚本完成，假设要获取锁的客户端id为1(需要保证id唯一性)，锁的name=“lock”（这个为需要同步的资源，一般减小粒度防止等待过久），首先判断“lock”是否存在，不存在则hset(id, name, 1),加锁成功，存在则判断hget(name)==id,相等则执行hincrby(key, name, 1)，对锁加一，即为可重入锁，否则加锁失败，加锁失败会返回key过期时间；同时，加锁成功还会设置锁的过期时间（默认30s），防止客户端在占有锁时断线，造成死锁。

```lua
if (redis.call('exists', KEYS[1]) == 0) then " +
   "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
   "redis.call('pexpire', KEYS[1], ARGV[1]); " +
   "return nil; " +
   "end; " +
"if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
    "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
    "redis.call('pexpire', KEYS[1], ARGV[1]); " +
    "return nil; " +
    "end; " +
"return redis.call('pttl', KEYS[1]);"
```

###### 解锁

解锁时同样需要判断是否本人持有锁，判断hget(name)==id，相等则hincrby(key, name, -1)，减1，如果值为0，则直接删除锁；

```lua
"if (redis.call('exists', KEYS[1]) == 0) then " +
"redis.call('publish', KEYS[2], ARGV[1]); " +
"return 1; " +
"end;" +
"if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
"return nil;" +
"end; " +
"local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
"if (counter > 0) then " +
"redis.call('pexpire', KEYS[1], ARGV[2]); " +
"return 0; " +
"else " +
"redis.call('del', KEYS[1]); " +
"redis.call('publish', KEYS[2], ARGV[1]); " +
"return 1; "+
"end; " +
"return nil;",
```



###### 看门狗

在设置锁的默认过期时间后，后续就没有判断锁的时效了，有可能锁已经失效了，但是业务没有执行完，此时其它客户端可以获取锁，造成两个客户端同时在处理业务，产生脏数据。watchDog就是另开一个定时任务线程，每隔一段时间检查锁的状态，如果还在持有就延长锁的时间。一般是一旦有客户端持有锁就启动看门狗线程。

###### 多实例锁失效

在集群模式下，假设一个客户端获取了锁，在主从复制的过程中master掉线了，这时候其中一个slave当选为master，此时这个master上没有锁，于是其它客户端可以获取到锁，此时，两个客户端都获取到了锁。

