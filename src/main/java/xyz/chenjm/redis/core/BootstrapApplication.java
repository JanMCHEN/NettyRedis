package xyz.chenjm.redis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.annotation.ClassPathCommandScanner;
import xyz.chenjm.redis.annotation.CommandScan;
import xyz.chenjm.redis.annotation.Source;
import xyz.chenjm.redis.command.DefaultRedisCommandHolder;
import xyz.chenjm.redis.config.PropertySource;
import xyz.chenjm.redis.command.RedisCommandHolder;
import xyz.chenjm.redis.core.handler.HandlerInit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import xyz.chenjm.redis.io.RedisAofAround;

import java.io.FileNotFoundException;
import java.io.IOException;

public class BootstrapApplication {
    private static Logger log = LoggerFactory.getLogger(BootstrapApplication.class);
    private int port = 7000;
    private int bossThreads = 1;
    private int workerThreads = 4;
    private int dbs = 16;

    public void setSource(PropertySource source) {
        this.source = source;
    }

    private PropertySource source;

    public void setBasePackages(String... basePackages) {
        this.basePackages = basePackages;
    }

    private String[] basePackages = new String[0];

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

    private EventLoop commandExecutor;
    private ServerBootstrap bootstrap;

    private RedisCommandHolder commands;


    @SuppressWarnings("all")
    public static BootstrapApplication run(Class<?> cls, String... args) {
        BootstrapApplication application = new BootstrapApplication();
        CommandScan scan = cls.getAnnotation(CommandScan.class);
        Source source = cls.getAnnotation(Source.class);
        if (scan != null) {
            application.setBasePackages(scan.value().length==0? scan.basePackages(): scan.value());
        }
        PropertySource propertySource = PropertySource.getDefaultPropertySource(source==null? null: source.value(), args);
        application.setSource(propertySource);
        application.run();
        return application;
    }

    protected void setBootstrap() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        bootstrap = new ServerBootstrap();
        commandExecutor = new DefaultEventLoop();
    }

    private void initDbs() {
        try {
            RedisDB2.init(dbs);
        } catch (IOException | ClassNotFoundException ignored) {

        }
    }

    private void initCommands(){
        commands = new DefaultRedisCommandHolder();
        ClassPathCommandScanner scanner = new ClassPathCommandScanner();
        scanner.setCommandFactory(commands);
        scanner.scan(basePackages);
        try {
            ((DefaultRedisCommandHolder) commands).addAround(new RedisAofAround("appendonly.aof"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void setProperties() {
        if(source==null) return;
        String port = source.getProperty("port");
        String dbs = source.getProperty("dbs");
        String bossThreads1 = source.getProperty("bossThreads");
        String workerThreads1 = source.getProperty("workerThreads");

        if (port != null) setPort(Integer.parseInt(port));
        if (dbs != null) setDbs(Integer.parseInt(dbs));
        if (bossThreads1!=null) setBossThreads(Integer.parseInt(bossThreads1));
        if (workerThreads1!=null) setWorkerThreads(Integer.parseInt(workerThreads1));

    }

    public void run() {
        setProperties();
        initDbs();
        initCommands();
        setBootstrap();
        doRun();
    }

    protected void doRun() {
        try {
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 通道handler初始化
            HandlerInit childHandler = new HandlerInit();
            childHandler.setCommandFactory(commands);
            childHandler.setEventLoop(commandExecutor);
            String debug = source.getProperty("debug");
            if(Boolean.parseBoolean(debug)) {
                childHandler.setLoggingHandler(new LoggingHandler(LogLevel.DEBUG));
            }
            bootstrap.childHandler(childHandler);

            ChannelFuture closeFuture = bootstrap.bind(port).sync().channel().closeFuture();
            log.info("listen on:{}", port);

            closeFuture.sync();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }



}
