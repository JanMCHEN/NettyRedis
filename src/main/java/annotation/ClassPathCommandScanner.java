package annotation;

import core.DefaultCommandFactory;
import core.RedisCommand;
import utils.PackageClassScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.Set;

public class ClassPathCommandScanner {
    private DefaultCommandFactory commandFactory;

    public void setCommandFactory(DefaultCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public void scan(String... basePackages) {
        PackageClassScanner scanner = new PackageClassScanner();
        scanner.addFilter(new PackageClassScanner.AnnotationTypeFilter(Command.class));
        scanner.scan(basePackages);
        Set<Class<?>> clazz = scanner.getClazz();
        for (Class<?> aClass : clazz) {
            try {
                newCommand((Class<RedisCommand>) aClass);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void newCommand(Class<RedisCommand> cls) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<RedisCommand> constructor = cls.getConstructor();
        RedisCommand redisCommand = constructor.newInstance();
        Command annotation = cls.getAnnotation(Command.class);
        commandFactory.addCommand(annotation.value(), redisCommand);
    }


}
