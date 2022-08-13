package bin;

import core.HandlerInit;
import core.RedisDB;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Server {

    static void serverCron() {
        // 每个数据库检查过期键
        RedisDB.removeFromExpire(20);
        // 每个数据库检查阻塞超时的键
        RedisDB.checkBlockedTimeout();

        if(RedisDB.isSaveNeed(60, 1)) {
            backGroundGroup.schedule(RedisDB.saveTask, 0, TimeUnit.SECONDS);
        }
    }

    static NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    static NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);

    public static EventExecutorGroup processGroup = new DefaultEventExecutorGroup(1);
    public static EventExecutorGroup backGroundGroup = new DefaultEventExecutorGroup(2);
    static {
        processGroup.scheduleAtFixedRate(Server::serverCron, 100, 100, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            processGroup.shutdownGracefully();
            backGroundGroup.shutdownGracefully();
            try {
                RedisDB.saveLast();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            bootstrap.childHandler(new HandlerInit());
            bootstrap.bind(7000).sync().channel().closeFuture().sync();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            System.out.println("爱关不关");
        }
    }
}

