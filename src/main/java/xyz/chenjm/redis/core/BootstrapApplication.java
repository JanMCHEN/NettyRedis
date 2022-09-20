package xyz.chenjm.redis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.annotation.ClassPathCommandScanner;
import xyz.chenjm.redis.annotation.CommandScan;
import xyz.chenjm.redis.annotation.Source;
import xyz.chenjm.redis.command.CommandExecutor;
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

public class BootstrapApplication {
    private static final Logger log = LoggerFactory.getLogger(BootstrapApplication.class);
    private int port = 7000;
    private int bossThreads = 1;
    private int workerThreads = 4;
    private int dbNums = 16;
    private PropertySource source;
    private String[] basePackages = new String[0];

    public void setSource(PropertySource source) {
        this.source = source;
    }

    public void setBasePackages(String... basePackages) {
        this.basePackages = basePackages;
    }

    public void setDbNums(int dbNums) {
        this.dbNums = dbNums;
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

    /**
     * 服务端状态管理
     */
    private RedisServer server;

    /**
     * redis命令执行器
     */
    private CommandExecutor cmdExecutor;
    private RedisCommandHolder commands;

    private RedisDBFactory dbFactory;


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
        eventLoop = new DefaultEventLoop();

        bootstrap.group(bossGroup, workerGroup);
    }

    private void initDbs() {

        dbFactory = RedisDBFactory.build(dbNums);
    }

    private void initServer() {
        server = new RedisServer();
        server.setEventLoop(eventLoop);
        server.setCmdExecutor(cmdExecutor);
        RedisDB[] dbs = new RedisDB[dbNums];
        for (int i=0;i<dbNums;++i) {
            dbs[i] = new RedisDB();
        }
        server.setDbs(dbs);

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
        if (dbs != null) setDbNums(Integer.parseInt(dbs));
        if (bossThreads1!=null) setBossThreads(Integer.parseInt(bossThreads1));
        if (workerThreads1!=null) setWorkerThreads(Integer.parseInt(workerThreads1));
    }

    public void run() {
        setProperties();
        initCommands();
        setBootstrap();
        initServer();
        doRun();
    }

    protected void doRun() {
        try {
            bootstrap.channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 通道handler初始化
            HandlerInit childHandler = new HandlerInit();
            childHandler.setServer(server);
            String debug = source.getProperty("debug");
            if(Boolean.parseBoolean(debug)) {
                childHandler.setLoggingHandler(new LoggingHandler(LogLevel.DEBUG));
            }
            bootstrap.childHandler(childHandler);

            ChannelFuture closeFuture = bootstrap.bind(port).sync().channel().closeFuture();
            log.info("listen on:{}", port);

            closeFuture.sync();
        } catch (Throwable e) {
            log.error("启动错误", e);
        }finally {
            close();
        }
    }

    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
