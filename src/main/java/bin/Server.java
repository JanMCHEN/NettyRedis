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
import java.util.concurrent.TimeUnit;

public class Server {

    static NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    static NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);

    public static EventExecutorGroup processGroup = new DefaultEventExecutorGroup(1);
    public static EventExecutorGroup backGroundGroup = new DefaultEventExecutorGroup(2);
    static {
        backGroundGroup.scheduleAtFixedRate(() -> {
            try {
                RedisDB.save(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 1, 60, TimeUnit.SECONDS);
        backGroundGroup.scheduleAtFixedRate(() -> {
            try {
                RedisDB.save(10);
            } catch (IOException ignored) {
            }
        }, 30, 300, TimeUnit.SECONDS);
        backGroundGroup.scheduleAtFixedRate(() -> {
            try {
                RedisDB.save(1);
            } catch (IOException ignored) {
            }
        }, 90, 900, TimeUnit.SECONDS);

        processGroup.schedule(() -> RedisDB.removeFromExpire(20), 100, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            bootstrap.childHandler(new HandlerInit());

            bootstrap.bind(7000).sync().channel().closeFuture().sync();
        } finally {
            System.out.println("爱关不关");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            processGroup.shutdownGracefully();
            backGroundGroup.shutdownGracefully();
            RedisDB.saveLast();
        }
    }
}

