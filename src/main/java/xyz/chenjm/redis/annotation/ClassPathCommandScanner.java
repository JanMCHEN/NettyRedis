package xyz.chenjm.redis.annotation;

import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.command.RedisCommandHolder;
import xyz.chenjm.redis.utils.PackageClassScanner;

import java.lang.reflect.InvocationTargetException;

public class ClassPathCommandScanner extends PackageClassScanner{
    private RedisCommandHolder commandFactory;

    public ClassPathCommandScanner() {
        addFilter(new PackageClassScanner.AnnotationTypeFilter(Command.class));
    }

    public void setCommandFactory(RedisCommandHolder commandFactory) {
        this.commandFactory = commandFactory;
    }

    @SuppressWarnings("unchecked")
    public void scan(String... basePackages) {
        super.scan(basePackages);
        for (Class<?> aClass : getClazz()) {
            try {
                commandFactory.addCommand((Class<RedisCommand>) aClass);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
