# NettyRedis
**Netty搭建Redis服务器** 

基于Netty框架搭建一个简易的Redis单机服务器，遵循RESP协议进行客户端-服务端通信，采用较新的redis多线程模型。
支持的命令持续更新中--------想把它写完但真没时间啊，慢慢写吧。

- 事务

  ```
  exec,multi, unwatch,watch,discard
  ```

  

- string

  ```
  get,set
  ```
  
- set

  ```
  sadd,srem,smembers,sismember,scard
  ```

  

[TOC]

## 简介

本着学习的态度，查看了redis部分源码，并学习了netty框架实现机制，这里记录一下学习过程。之前写过用Python实现一个简易版的redis服务器，当时没有考虑redis底层数据结构的实现方式，以及众多细节，全凭感觉实现了部分功能，也暴露出了当时的无知。这里将参考redis3.0版本的源码，重点学习redis底层数据结构，线程模型，并结合netty用Java语言实现一个简单的Redis数据库，服务器的实现代码在src目录下。

## 实现细节

### 数据结构设计

首先redis是一个k-v形式存储的数据库，典型的NoSql，直接在内存中读取，所以速度奇快，瓶颈可以认为就是网络通信这块的延迟，以至于redis在前面的版本中直接放弃了多线程的设计方案。redis设计了一个字典，用来存储所有类型的数据，默认设置下有16个数据库，redis内部维护了一个dict类型的数组，数组长度即为数据库个数，键值类型都是redisObject，结构体为：

```c
typedef struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
    unsigned lru:REDIS_LRU_BITS; /* lru time (relative to server.lruclock) */
    int refcount; // 引用计数
    void *ptr;    // 不同type的数据，统一用void*指针定义，类似Java里的Object

} robj;
```

type字段用4位对齐，在dict存储不同的数据类型是就是靠type区分，共5种宏定义，对应5种数据结构：

```c
/* Object types */
#define REDIS_STRING 0
#define REDIS_LIST 1
#define REDIS_SET 2
#define REDIS_ZSET 3
#define REDIS_HASH 4
```

encoding字段对应每种数据结构的编码类型，每次创建一个robj时会尝试更节省空间的编码类型，把空间节省到了极致，Java想实现这就显得有点力不从心了，encoding字段也是4位对齐，共9种宏定义。同时，这也是c语言在实现类似多态方面的妥协，在面向对象的语言中，这部分可以省略，只提供每种类型数据操作的接口就可以。但是，这可以作为c语言面向对象的一种参考，Java实现中也可以保留。

```c
/* Objects encoding. Some kind of objects like Strings and Hashes can be
 * internally represented in multiple ways. The 'encoding' field of the object
 * is set to one of this fields for this object. */
#define REDIS_ENCODING_RAW 0     /* Raw representation */
#define REDIS_ENCODING_INT 1     /* Encoded as integer */
#define REDIS_ENCODING_HT 2      /* Encoded as hash table */
#define REDIS_ENCODING_ZIPMAP 3  /* Encoded as zipmap */
#define REDIS_ENCODING_LINKEDLIST 4 /* Encoded as regular linked list */
#define REDIS_ENCODING_ZIPLIST 5 /* Encoded as ziplist */
#define REDIS_ENCODING_INTSET 6  /* Encoded as intset */
#define REDIS_ENCODING_SKIPLIST 7  /* Encoded as skiplist */
#define REDIS_ENCODING_EMBSTR 8  /* Embedded sds string encoding */
```

这一部分用Java作了简单的实现，Java对象的存储机制，这里把type和encoding定义成int，并合在一块描述，不定义成byte是因为对象会以8个字节填充，定义成byte无意义，并且在进行运算的时候还会多生成一个int转byte的字节码。并且作了很多简化，只实现了编码类型在long和字符串之间的转化。注意这里数据类型是Object，意味着即使转成long，也是包装类型的Long，这里没用泛型本质上是一样的。包装类型的Long就丧失了原本long只需8个字节存储的优势，仍然是以对象的形式存储，且后续运算的时候还有拆箱装箱的过程，效率肯定没有单纯的long类型高。Java没有指针这个概念，也就没有了void*的便捷，使用Object也是无奈之举。

```java
public class RedisObject implements Serializable {
    public final static int OBJ_STRING = 0;
    public final static int OBJ_LIST   = 1;
    public final static int OBJ_SET    = 2;
    public final static int OBJ_ZSET   = 3;
    public final static int OBJ_HASH   = 4;

    public final static int OBJ_ENCODING_RAW       =  0;
    public final static int OBJ_ENCODING_INT       =  1;

    private int typeEncoding;     // type and encoding,实际只需用到8位就能存储，高4位存encoding，低4位存type
//    private long lru;
//    private int refCount;
    private Object ptr;
}
```

可以看到，在实现上，c语言没有垃圾自动回收机制，redis自己采用了引用计数法实现。而Java的实现就简单许多，由于不同的垃圾回收机制，性能上会有差异。

#### string

string是redis用到最多的数据结构，其它几种数据结构也大多以string作为其内部结构。

### 事务实现

Redis的事务实现的十分简结，只有相关的5条命令，在源文件multi.c中可以看到相关的命令实现。具体的，它依赖于一个redisClient结构体，为每一个接入的客户端保存了状态，里边定义了所有状态相关的数据。其中与事务相关的有:

```c
typedef struct redisClient {
    // 事务状态等
    int flags;  /* REDIS_SLAVE | REDIS_MONITOR | REDIS_MULTI ... */
    multiState mstate;  // 事务保存
    list *watched_keys; // 该客户端监控的锁
    ...;
    
} redisClient;
typedef struct multiState {
    // 保存需要连续执行的命令
    multiCmd *commands;     /* Array of MULTI commands */
    int count;              /* Total number of MULTI commands */
    int minreplicas;        /* MINREPLICAS for synchronous replication */
    time_t minreplicas_timeout; /* MINREPLICAS timeout as unixtime. */
} multiState;

// redis.h中的redisDb结构体中还有一个字典结构保存该数据库中所有被监视的键
// 结构<key, list<redisClient>>,将每个被监视的键加入字典，这个结构主要是为了后续判断是否对监控的键进行了写操作
dict *watched_keys;
```

主要代码逻辑在multi.c中，这里总结一下Redis的事务性质

1. watch监控一些键，体现在redisClient和redisDb中往watched_keys添加，同时，只能在multi命令之前，即不能在事务执行过程中watch

2. unwatch是与watch相对的命令，它取消监控该客户端所有键，并且不能单独取消监控某一个键，（按道理来说这也很好实现，但考虑到watched_keys是一个链表结构，从中间删除复杂度比较高吧，没有实现）。还有一点值得注意的是它可以在multi命令中执行，并且结果总是ok。(额，我也不知道为甚么要这么设计，明明可以和watch保持一致，在multi中执行意味着exec后unwatch没有起到丝毫作用，最后总是会unwatch的)。

3. multi 进入事务执行，当然，不能在exec之前再次multi。执行multi后，所有的非事务命令(包括unwatch)都会加入commands数组，直到遇到exec，将所有命令以此执行，以数组(Resp协议中)的形式返回结果。或者discard取消执行，并清空commands数组

4. exec 显然，只有在multi状态时才能执行exec，在执行commands中的命令前，首先检查是否在multi后是否有错误的命令，这种错误包括命令不存在、命令参数不正确等一些可以脱离数据库就能检测出的错误，但是对于一些需要string转long发生错误的这一阶段不检测（Java实现过程中参数检测更加严格）。总之，redis官方建议在执行事务过程中调用者应确保不会发生一些错误的命令，否则应该在生产环境之前纠正。检查出错误时执行exec会返回一个错误信息。除了检查错误，还会检查是否有别的客户端（严格来说，watch key后，在执行multi之前任意修改key的行为都会检测，包括自己的修改行为；在multi执行后，exec执行之前其它客户端在key上的修改行为会被检查）修改过watch的键，一旦修改，所有commands都不会执行，并返回一个null（Resp中的nil）。正常执行时，会依次执行commands中的命令，并将结果按顺序添加进数组返回，哪怕在执行过程中遇到错误，还会继续向后执行，不支持回滚（官方说法是实现起来还需要保存回滚日志，实现起来比较复杂，不符合redis的简单理念），执行完后重置。

5. discard 在multi后执行，直接退出事务状态，重置，返回ok。

6. 重置包括清空commands数组，清空redisClient的watched_keys(同时移除redisDb中关联的)。

7. redis在对数据库操作时是单线程的，不需要加锁。这也保证了commands中的命令可以连续执行而不会被打断，保证了原子性，并且不需要额外的加锁操作。

8. 客户端断开连接时，会清空redisClient监控的键。

Java实现中RedisClient中保存了客户端状态，完整实现了redis事务。

