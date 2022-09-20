package xyz.chenjm.redis.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class RedisClient {
    ClientHandler handler = new ClientHandler();
    private Channel channel;
    Bootstrap bootstrap;
    NioEventLoopGroup executors;
    String host = "localhost";
    int port = 6379;

    public RedisClient(){}

    public RedisClient(String h, int p) {
        host = h;
        port = p;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void setExecutors(NioEventLoopGroup executors) {
        this.executors = executors;
    }

    public void connect() throws InterruptedException {
        if (bootstrap == null)
            bootstrap = new Bootstrap();
        if (executors == null)
            executors = new NioEventLoopGroup(1);

        bootstrap.group(executors);

        channel = bootstrap.channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new RedisEncoder())
                        .addLast(new RedisDecoder())
                        .addLast("string-decode", new RedisBulkStringAggregator())
                        .addLast("array-decode", new RedisArrayAggregator())
                        .addLast(handler);
            }
        }).connect(host, port).sync().channel();
        channel.closeFuture().addListener(future -> executors.shutdownGracefully());
    }

    public void close() {
        channel.close();
    }

    public RedisConnection newConnection() {
        return new DefaultConnection();
    }

    class DefaultConnection implements RedisConnection{
        public volatile int connId = -1;
        public Thread thread = Thread.currentThread();

        @Override
        public Object sendCommand(Object... args) throws InterruptedException {

            final ChannelPromise promise = channel.newPromise();

            promise.addListener(future -> {
                if (future.isSuccess()) {
                    setConnId(handler.getWriteIdx()-1);
                }
            });
            channel.writeAndFlush(args, promise);

            // sync返回时监听器不一定执行
            promise.sync();

            int idx = getConnId();

            try {
                return handler.get(idx);
            } catch (Exception e) {
                System.out.println(idx);
                throw new RuntimeException(e);
            }
        }

        /**
         * connId==-1时无效状态，确保监听器一定执行
         * @return 一次性的connId
         */
        private int getConnId(){
            while (connId < 0) {
                LockSupport.park();
            }
            int id = connId;
            connId = -1;
            return id;
        }

        /**
         * 监听器异步调用，确保connId被设置值
         */
        private void setConnId(int connId) {
            this.connId = connId;
            LockSupport.unpark(thread);
        }
    }

}
