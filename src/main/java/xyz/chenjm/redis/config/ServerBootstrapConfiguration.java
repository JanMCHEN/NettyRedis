package xyz.chenjm.redis.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.core.RedisServer;
import xyz.chenjm.redis.core.handler.HandlerInit;

public class ServerBootstrapConfiguration implements RedisConfig{
    private static final Logger log = LoggerFactory.getLogger(ServerBootstrapConfiguration.class);
    int port = 7000;
    int bossThreads = 1;
    int workerThreads = 4;

    boolean debug;

    private RedisServer server;

    public void setServer(RedisServer server) {
        this.server = server;
    }

    public ChannelFuture newChannelFuture() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
        bootstrap.group(bossGroup, workerGroup).channel(ServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        HandlerInit initializer = new HandlerInit();
        initializer.setServer(server);
        if (debug) {
            initializer.setLoggingHandler(new LoggingHandler(LogLevel.DEBUG));
        }
        bootstrap.childHandler(initializer);

        try {
            ChannelFuture cf = bootstrap.bind(port).sync().channel().closeFuture();
            log.info("listening on {}", port);
            cf.addListener(future -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
            return cf;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
