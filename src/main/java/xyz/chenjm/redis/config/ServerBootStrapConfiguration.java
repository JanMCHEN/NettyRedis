package xyz.chenjm.redis.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBootStrapConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ServerBootStrapConfiguration.class);
    int port = 7000;
    int bossThreads = 1;
    int workerThreads = 4;

    public ServerBootstrap newServerBootStrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
        bootstrap.group(bossGroup, workerGroup);
//        bootstrap.childOption();

        return bootstrap;

    }


}
