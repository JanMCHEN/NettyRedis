import core.*;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.RedissonReadLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SomeTest {
    @Test
    public void testIntSeq() {
        IntSet intSet = new IntSet();
        RedisList.IntList intList = new RedisList.IntList();
        Random random = new Random();
        for(int i=0;i<1000000;++i) {
            intList.add(random.nextLong());
        }
        System.out.println(intList.size());
    }

    @Test
    public void testList() {
        RedisList redisList = new RedisList();
        LinkedList<Object> longs = new LinkedList<>();
        Random random = new Random();
        long tic = System.currentTimeMillis();
        for(int i=0;i<10000000;++i) {
            redisList.addFirst(random.nextLong());
            int i1 = random.nextInt();
            if(i1 %8==0) {
//                redisList.addFirst(("a"+i1));
            }
        }
        long toc = System.currentTimeMillis();
        System.out.println(toc-tic);
        System.out.println(redisList.size());


//        LinkedList<Long> longs = new LinkedList<>();
//        for(int i=0;i<100000000;++i) {
//            longs.addFirst(random.nextLong());
//        }
//        System.out.println(longs.size());
    }

    @Test
    public void testString() {
//        System.out.println(Arrays.toString("中".getBytes()));
//        RedisString.RawString rawString = new RedisString.RawString(new byte[]{1, 2});
        int length = 1000000;
        String[] s1 = new String[length];
        RedisString.RawString[] s2 = new RedisString.RawString[length];

        long t1 = System.currentTimeMillis();

        String base = "我爱大家按时到货就卡死的急啊卡的纠结啊jjjjhhjjjjjjjjjjjjaaaaaaaaaaaaalajdkaljdkadkjsjsjkjjhj";

        for(int i=0;i<length;++i) {
            s1[i] = base + i;
//            s2[i] = new RedisString.RawString(base+i);
        }

        long t2 = System.currentTimeMillis();

        HashSet<String> set1 = new HashSet<>(Arrays.asList(s1));

//        HashSet<RedisString.RawString> set2 = new HashSet<>(Arrays.asList(s2));
        long t3 = System.currentTimeMillis();
        System.out.println(length+" : t="+(t3-t2));
//        System.out.println(set1.size());
    }

    @Test
    public void test() {
        new ConcurrentHashMap<>();
    }

    class A{
        private void a() {
            System.out.println('a');
        }
        public void b() {

        }
    }

    class B extends A {
        public void a() {
            System.out.println('b');
        }
        public void b () {

        }
    }

    @Test
    public void test1() throws Throwable {
//        int []nums =  {1,2,3,4};
//        int []weights = {20,35,25,20};
//        new HashMap<>();
        Thread a = new Thread(() -> {
            System.out.println(Thread.currentThread().getName());
        }, "a");
        a.start();
        Thread a1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName());
        }, "a");
        a1.start();
        TimeUnit.SECONDS.sleep(20);
    }

    @Test
    public void redis() {
        RedissonClient client = Redisson.create();
        RLock lock = client.getLock("a");
        lock.lock();

    }
}
