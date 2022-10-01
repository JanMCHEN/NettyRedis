package xyz.chenjm.redis.client;

public class ReplicatorTest {
    public static void main(String[] args) throws InterruptedException {
        RedisCli cli = new RedisCli();
        cli.connect();

        RedisConnection conn = cli.newConnection();

        Object obj = conn.sendCommand("psync", "?", "-1");

        System.out.println(obj);

    }
}
