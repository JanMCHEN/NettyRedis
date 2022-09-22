package xyz.chenjm.redis.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import xyz.chenjm.redis.handler.HandlerInit;

public class RedisReplicator {
    RedisServer server;
    Channel channel;

    NioEventLoopGroup executors = new NioEventLoopGroup(1);

    String host;
    int port;

    long offset = -1;
    String masterId = "0000";


    public RedisReplicator() {

    }

    public void run() {
        try {
            prepare();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void prepare() throws InterruptedException {
    }

    void connect() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(executors);

        HandlerInit init = new HandlerInit();
        init.setServer(server);

        channel = bootstrap.channel(NioSocketChannel.class).handler(init).connect(host, port).sync().channel();
        channel.closeFuture().addListener(future -> connect());

    }


}
