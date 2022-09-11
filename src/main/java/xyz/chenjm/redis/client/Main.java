package xyz.chenjm.redis.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup executors = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();

        Channel channel = bootstrap
                .group(executors)
                .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new RedisEncoder())
                                .addLast(new ClientHandler());
                    }
                }).connect("localhost", 7000).sync().channel();

        channel.writeAndFlush(new String[]{"get", "a"});

        channel.read();

        channel.closeFuture().sync();

        executors.shutdownGracefully();
    }
}
