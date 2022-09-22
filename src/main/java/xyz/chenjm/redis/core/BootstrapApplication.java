package xyz.chenjm.redis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.annotation.ClassPathCommandScanner;
import xyz.chenjm.redis.annotation.CommandScan;
import xyz.chenjm.redis.annotation.Source;
import xyz.chenjm.redis.command.*;
import xyz.chenjm.redis.config.PropertySource;
import xyz.chenjm.redis.config.RedisServerConfiguration;
import xyz.chenjm.redis.config.ServerBootstrapConfiguration;

public class BootstrapApplication {
    private static final Logger log = LoggerFactory.getLogger(BootstrapApplication.class);
    private PropertySource source;
    private String[] basePackages = new String[0];

    public void setSource(PropertySource source) {
        this.source = source;
    }

    public void setBasePackages(String... basePackages) {
        this.basePackages = basePackages;
    }


    private final RedisServerConfiguration serverConf = new RedisServerConfiguration();
    private final ServerBootstrapConfiguration sbc = new ServerBootstrapConfiguration();

    private final ClassPathCommandScanner scanner = new ClassPathCommandScanner();

    private final CommandHolder cmdHolder = new DefaultCommandHolder();

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

    public void run() {
        // 扫描并添加Command
        scanner.scan(basePackages);
        scanner.getScans().forEach(aClass -> {
            cmdHolder.addCommand(scanner.newCommand(aClass));
        });

        serverConf.initFromSource(source);
        serverConf.setCommandHolder(cmdHolder);
        RedisServer server = serverConf.newServer();

        sbc.initFromSource(source);
        sbc.setServer(server);

        try {
            sbc.newChannelFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
