package core;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;

public class BootstrapApplication {
    private int port = 7000;
    private int bossThreads = 1;
    private int workerThreads = 1;
    private int dbs = 16;

    public void setDbs(int dbs) {
        this.dbs = dbs;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private EventLoop eventLoop;
    private ServerBootstrap bootstrap;

    private CommandFactory commands;

    public BootstrapApplication() {
    }
    protected void setBootstrap() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
    }

    private void initDbs() {
        try {
            RedisDB.init(dbs);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void initCommands(){

    }



    public void run(String... args) {
        initDbs();
        initCommands();
        setBootstrap();
    }



}
