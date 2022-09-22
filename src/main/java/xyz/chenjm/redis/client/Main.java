package xyz.chenjm.redis.client;

import java.util.Map;
import java.util.concurrent.*;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        RedisCli client = new RedisCli("localhost", 7000);
        client.connect();
        int threads = 5000;

        ExecutorService service = Executors.newFixedThreadPool(threads);
        CountDownLatch countDownLatch = new CountDownLatch(threads);

        Map<RedisConnection, Thread> map = new ConcurrentHashMap<>();

        for (int i=0;i<threads;++i) {
            service.submit(()->{
                RedisConnection connection = client.newConnection();
                if (map.containsKey(connection)) {
                    System.out.println(connection);
                }
                map.put(connection, Thread.currentThread());
                for (int j=0;j<5;++j) {
                    try {
                        Object x = connection.sendCommand("get", "test");
                        System.out.println(x);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                countDownLatch.countDown();
            });
        }
        boolean await = countDownLatch.await(30, TimeUnit.SECONDS);
        if (await) {
            client.close();
        }
        System.out.println(countDownLatch);
        service.shutdown();
    }
}
