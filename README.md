# NettyRedis
**Netty搭建Redis服务器** 

基于Netty框架搭建一个简易的Redis单机服务器，遵循RESP协议进行客户端-服务端通信，采用较新的redis多线程模型。
支持的命令持续更新中--------

- db

  ```
  save,bgsave,select,flushdb,flushall
  ```

  

- keys

  ```
  expire,expireat,ttl,persist,keys,exists,del,type
  ```

  

- 事务

  ```
  exec,multi, unwatch,watch,discard
  ```

  

- string

  ```
  get,set，setbit,getbit,bitcount,incr,incrby,append,strlen
  ```
  
- set

  ```
  sadd,srem,smembers,sismember,scard
  ```

  
  
- list

  ```
  lpush,rpush,lpop,rpop,llen,lrange,blpop,brpop,rpoplpush,brpoplpush,lpushx,rpushx
  ```

  
  
- 

  

[TOC]

## 简介

本着学习的态度，查看了redis部分源码，并学习了netty框架实现机制，这里记录一下学习过程。之前写过用Python实现一个简易版的redis服务器，当时没有考虑redis底层数据结构的实现方式，以及众多细节，全凭感觉实现了部分功能，也暴露出了当时的无知。这里将参考redis3.0版本的源码，重点学习redis底层数据结构，线程模型，并结合netty用Java语言实现一个简单的Redis数据库，服务器的实现代码在src目录下。

## 实现细节

### 数据结构设计

借鉴了redis源码设计，同时，Java提供了良好的面向对象设计，内存分配和垃圾回收可以不用手动操作，减少了源码中关于引用计数、类型、编码等字段，只提供了一个接口RedisObject用以标识redis的数据结构。

```java
public interface RedisObject extends Serializable {
    static int getType(RedisObject obj) {
        if(obj instanceof RedisString) return 0;
        if(obj instanceof RedisList) return 1;
        if(obj instanceof RedisSet) return 2;
        return 5;
    }
    static RedisString.RedisInt valueOf(long value) {
        return RedisString.RedisInt.valueOf(value);
    }
    static RedisString valueOf(byte[] b) {
        try {
            long v = RedisString.parseLong(b, b.length);
            return valueOf(v);
        } catch (NumberFormatException e) {
            return new RedisString.HashString(b);
        }
    }
}
```

#### string

string是redis用到最多的数据结构，其内部作了好几种不同的编码类型，主要有表示整型(long)的，embstr(简单字符串)，rawstr(长字符串，涉及到修改操作时都会转成这种类型)，具体实现不在这里分析。为了尽量节省空间，redis在存储时会优先选择更节约的编码方式，典型的以空间换时间。由于Java原生String有诸多限制，首先它不可变，无法获取内部byte数组(可以复制一份返回)，内部还有一些编码转换，与redis客户端不兼容，相比于直接操作bytes开销更大，考虑自己实现string类型，以支持bitmap位操作。

代码比较长，总共定义了4种string。

1. 不可变的RedisInt，在新添加一个键值对时，如果可以转成long类型会用这种表示。
2. 可变的IntString，在进行incr等操作时，会考虑转成这种存储，没有重写hashcode方法，所以不建议当作字典的键，也因为可变对象不应该当作键，容易造成内存泄露（Python可散列对象即为不可变对象）
3. 不可变的HashString，当无法表示成long类型时，默认会用这种类型存储，重写了hashcode方法，表现的更像Java中的String
4. 可变的RawString，一旦发生修改行为，比如append，setbit等操作，会转成这种类型，内部会动态扩容，小于1M时每次扩容两倍，大于1M则每次加1M

string是作为其它结构的键值，当然，其它结构也有“空间压缩版”。

#### set

redis中set实现有两种结构，更省空间的intset和hash。intset用数组存储，只能存储整型数据，当往intset添加一个非整数据或超出预设的最大长度时会转成hash。这里主要说明一下intset的实现，用Java实现如下：

```java
// 根据数值大小考虑用几个byte存储一个值
protected static final int ENC_INT16 = 2;  // char
protected static final int ENC_INT32 = 4;  // int
protected static final int ENC_INT64 = 8;  // long

protected static final int INT16_MAX = 0x8fff;
protected static final int INT16_MIN = -0x8fff-1;

protected static final int MAX_LENGTH = 512; // 最大长度

protected int encoding;     // 编码方式，char，int，long
protected int length;       // 实际存放数据的长度，=contents.length/encoding
protected byte[] contents;  // 数据容器
```

以上的字段为一个整型数据用byte[]存储时的类型定义，具体的实现主要看add方法，将一个数值添加到容器指定位置

```java
public boolean add(int pos, long value) {
        if(length == MAX_LENGTH) {
            return false;
        }
        int enc = valueEncoding(value);
        byte[] newContents;
        if(enc > encoding) {
            // 编码升级
            newContents = new byte[(length+1) * enc];
            for(int i=0;i<length;++i) {
                set(newContents, i, enc, get(i));
            }
            contents = newContents;
            encoding = enc;
        }
        else {
            newContents = Arrays.copyOf(contents, (length+1) * encoding);
        }
        if (pos<length) {
            int cur = pos * encoding;
            System.arraycopy(contents, cur, newContents, cur + encoding, newContents.length - encoding - cur);
        }
        set(newContents, pos, encoding, value);
        contents = newContents;
        length ++;
        return true;
    }
```

在IntSet的实现中，要保证集合的不重复性，需要将数据排序，也有利于元素的查询（O(logn)时间复杂度）,在实现add时候需要按元素排序，同时不插入相同元素；

```
public boolean add(long value) {
        int pos = insertPos(value);
        if(pos==-1) {
            // 数组为空，直接新建一个数组插入
            length ++;
            contents = new byte[encoding];
            set(0, value);
            return true;
        }
        if(pos < length && get(pos)==value) {
            // 已经存在，不插入
            return false;
        }

        return add(pos, value);
    }
```

#### list

list是一个双向链表的结构，但redis为了省空间，又玩了很多花样。内部的ziplist将数据进行连续存储，以数组的形式可以开辟连续内存，同时有利于加载，有诸多好处，但每次插入删除数据时需要重新开辟空间，在数据量大时难以忍受。基于链表的存储可以直接修改指针，在redis之前的版本时数据量较大时会用双向链表存储，类似于LinkedList，但是，后来redis又提出了一个quicklist，里边元素放的是ziplist，这样就结合了数组和链表的优点。	但实现起来复杂度蹭蹭蹭就上去了，特别是ziplist,暂时不考虑用Java实现。为了有这种思想呢，也结合了一下前面的Int容器，存整型时直接往里添加，非整型时重新加一个链表节点。存取数据时要判断当前类型是不是int容器。在实现阻塞操作时不是在数据结构里实现的，在具体的命令再实现。

#### skiplist

redis中用于排序的数据结构，网上一搜跳表就能看到类似redis跳表原理之类的，搞得都以为是redis提出来的。

跳表原理不复杂，但要实现理想的跳表简直不可能，现在的跳表都是基于随机数来实现的。Java中也有一个ConcurrentSkipListMap的实现，显然是一个线程安全类，当然实现线程安全是要一定时间代价的，并且其中的键值都是用Object定义的，空间不友好，还有一个无法与redis相容的重要原因是不允许重复键。无奈只能自己设计一个满足需求的SkipList，并且不需要线程安全保证，可以简化很多。网上代码怎么说呢，没找到好的，在了解了实现原理之后看ConcurrentSkipListMap源码其实还是不好懂，简单的put操作为了线程安全饶了好多弯子，为了无锁化，好多CAS操作，属实看的头疼。硬着头皮啃了一下，把其中的插入和删除啃出来了。里边对随机数的应用非常值得学习，附上我实现的部分代码

```java
package core;

import java.util.Random;

/**

 * */
public class SkipList <T> {
    private static final int MAX_LEVEL = 62;  // long类型总共64位，除去先验1/4判断用掉两位，最多能表示62位的概率
    static final class Node<T> {
        final int key;
        T val;
        Node<T> next;
        Node(int key, T value, Node<T> next) {
            this.key = key;
            this.val = value;
            this.next = next;
        }
    }
    static final class Index<T> {
        Node<T> node;  // currently, never detached
        final Index<T> down;
        Index<T> right;
        Index(Node<T> node, Index<T> down, Index<T> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }
    }
    private static final Random RANDOM = new Random();

    private Index<T> head;
    private long length;

    public SkipList() {
    }
    public boolean add(int key, T value) {
        Node<T> node = new Node<>(key, value, null);
        length ++;  // always success
        if(this.head==null) {
            // first add
            Node<T> baseNode = new Node<>(Integer.MAX_VALUE, null, node);
            head = new Index<>(baseNode, null, null);
            return true;
        }
        int level = 0;      // 当前层为0层
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
                level ++;
            }
            else {
                break;
            }
        }
        insert(cur, node);
        newIndex(node, level);
        return true;
    }
    public boolean remove(int key, T value) {
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if(key < next.node.key)
                    break;
                if (key > next.node.key)
                    cur = next;
                else if(next.node.val.equals(value)) {  // key&&val 相等
                    doRemove(cur);
                    return true;
                }
                else                                       // 值不相等只能比较下一级
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
            }
            else
                break;
        }
        // 没有找到，需要从cur的node节点开始找,只需删除node
        for(Node<T> c=cur.node; c.next!=null; c=c.next) {
            if(c.next.key > key)
                break;
            if(c.next.key==key && c.next.val.equals(value)) {
                c.next = c.next.next;
                if(cur.node==head.node) { // 删除的是第一个节点
                    cur = this.head;
                    while(cur != null) {       // 删除头节点时直接修改next指针有可能right有重复值
                        if(cur.right!=null && cur.node==cur.right.node) {
                            cur.right = cur.right.right;
                            break;
                        }
                        if(cur.right==null) {
                            cur = cur.down;
                        }
                        else {
                            cur = cur.right;
                        }
                    }
                    tryReduce();
                }
                length --;
                return true;
            }
        }

        return false;
    }
    private void doRemove(Index<T> pre) {
        Index<T> del = pre.right;
        Index<T> cur=pre;
        Node<T> delNode = del.node;
        for(Index<T> next=pre; next != null; next=next.down,del=del.down) {   // 向下 删除
            while (next.right !=del) {
                cur = next;
                next = next.right;
            }
            next.right = del.right;

        }
        // unlink node
        Node<T> n=cur.node;
        while (n.next != delNode) {
            n = n.next;
        }
        n.next = n.next.next;
        length--;
        tryReduce();

    }
    private void tryReduce() {
        Index<T> head = this.head;
        while (head != null && head.right==null && head.down != null) {      // 降级
            head = head.down;
        }
        this.head = head;
    }
    private Index<T> findPredecessor(int key) {
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
            }
            else {
                break;
            }
        }
        return cur;
    }

    private void insert(Index<T> index, Node<T> node) {
        Node<T> cur = index.node;
        while(cur.next !=null) {
            if(cur.next.key > node.key){
                break;
            }
            cur = cur.next;
        }
        node.next = cur.next;
        cur.next = node;

    }
    private void newIndex(Node<T> z, int level) {
        int lr = RANDOM.nextInt();
        Index<T> x = null;
        if((lr & 0x03) == 0) {   // 1/4 prob
            int hr = RANDOM.nextInt();
            long rnd = ((long)hr << 32) | ((long)lr & 0xffffffffL);  // 两个int拼接成long
            for (;;) {               // create at most 62 indices
                x = new Index<T>(z, x, null);
                if (rnd >= 0L || --level < 0)       // 1/2 prob，每次左移相当于判断最高位是否为1，1则是负数
                    break;
                else                                // 抽中1，同时判断是否到最顶端，往下走，左移
                    rnd <<= 1;
            }
        }
        if(x == null) return;
        addLevel(x, level);

    }
    private void addLevel(Index<T> newIndex, int skip) {
        Index<T> cur = head;
        int key = newIndex.node.key;
        if(skip == -1) { // 更新head
            head = new Index<>(cur.node, cur, null);
        }
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if (skip > 0) {
                skip--;
            } else { // 更新right指针
                Index<T> right = cur.right;
                cur.right = newIndex;
                newIndex.right = right;
                newIndex = newIndex.down;
            }
            if((cur = cur.down) == null) {
                assert newIndex == null;   // 确保操作正确
                break;
            }
        }
    }
    public int getLevel() {
        return getLevel(this.head);
    }
    public int getLevel(Index<T> head) {
        int level = 0;
        for (Index<T> cur=head;cur!=null;cur=cur.down) {
            level++;
        }
        return level;
    }
    public void usageLevel() {
        if(head==null || head.node.next==null){
            System.out.println("Empty");
            return;
        }
        int level = getLevel();
        StringBuilder ans = new StringBuilder();
        for (Index<T> cur=head;cur!=null;cur=cur.down) {
            ans.append("level=").append(level--).append(":[");
            int count = 0;
            for (Index<T> cur1=cur;cur1!=null;cur1=cur1.right) {
                if(count++>10) {
                    continue;
                }
                Node<T> node = cur1.node;
                if(count==1) {
                    node = node.next;
                }
                ans.append(node.val).append(",");
            }
            ans.append("]").append(count).append("\n");
        }
        ans.append("level=").append(level).append(":[");
        int count = 0;
        for(Node<T> node=head.node.next; node !=null;node=node.next) {
            if(count++>10) {
                continue;
            }
            ans.append(node.val).append(",");
        }
        ans.append("]").append(count).append("\n");
        System.out.println(ans);
    }
    public long length() {
        return length;
    }
    public boolean isEmpty() {
        return length == 0;
    }
    public int countBetween(int start, int stop) {
        if(isEmpty()) return 0;
        Node<T> node = findPredecessor(start).node;
        int count = 0;
        while(node != null && node.key < start) {
            node = node.next;
        }
        while (node != null && node.key <= stop) {
            node = node.next;
            count++;
        }
        return count;
    }


    @Override
    public String toString() {
        if(head==null){
            return "";
        }
        StringBuilder ans = new StringBuilder();
        Node<T> node = head.node;
        while(node != null) {
            ans.append(node.key).append(":").append(node.val);
            node = node.next;
            if(node==null) break;
            ans.append(", ");
        }
        return ans.toString();
    }

}

```



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
8. 在事务中执行阻塞操作时为避免死锁，不阻塞。
9. 客户端断开连接时，会清空redisClient监控的键。

Java实现中RedisClient中保存了客户端状态，完整实现了redis事务。

