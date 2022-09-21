package xyz.chenjm.redis.annotation;

import xyz.chenjm.redis.command.CommandRunner;
import xyz.chenjm.redis.utils.PackageClassScanner;

import java.lang.reflect.InvocationTargetException;

public class ClassPathCommandScanner extends PackageClassScanner{
    public ClassPathCommandScanner() {
        addFilter(new PackageClassScanner.AnnotationTypeFilter(Command.class));
        addFilter(new PackageClassScanner.TypeFilter(CommandRunner.class));
    }

    public CommandRunner newCommand(Class<?> cls) {
        try {
            return (CommandRunner) cls.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
