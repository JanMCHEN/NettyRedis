package core;

import annotation.ClassPathCommandScanner;
import annotation.CommandScan;
import annotation.Source;
import config.PropertySource;
import core.handler.CommandHandler;
import core.handler.HandlerInit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;

public class BootstrapApplication {
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
    private ServerBootstrap bootstrap;

    private CommandFactory commands;

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
    }

    private void initDbs() {
        try {
            RedisDB.init(dbs);
        } catch (IOException | ClassNotFoundException ignored) {

        }
    }

    private void initCommands(){
        commands = new DefaultCommandFactory();
        ClassPathCommandScanner scanner = new ClassPathCommandScanner();
        scanner.setCommandFactory((DefaultCommandFactory) commands);
        scanner.scan(basePackages);
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
            CommandHandler commandHandler = new CommandHandler();
            commandHandler.setCmdFactory(commands);
            childHandler.setCmdHandler(commandHandler);
            bootstrap.childHandler(childHandler);

            ChannelFuture closeFuture = bootstrap.bind(port).sync().channel().closeFuture();
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