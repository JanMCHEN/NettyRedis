package xyz.chenjm.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public interface RedisConfig {
    Logger log = LoggerFactory.getLogger(RedisConfig.class);

    default void initFromSource(PropertySource source) {
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isPrivate(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                setField(field, source);
            } catch (IllegalAccessException e) {
                log.warn("field '{}'set wrong", field.getName(), e);
            }
        }
    }
    default void setField(Field field, PropertySource source) throws IllegalAccessException {
        Class<?> type = field.getType();
        String value = source.getPropertyIgnoreCase(field.getName());
        if (type.isAssignableFrom(Number.class)) {
            field.set(this, Long.valueOf(value));
        } else if (type.isAssignableFrom(Boolean.class)) {
            field.set(this, Boolean.parseBoolean(value));
        } else if (type.isAssignableFrom(String.class)) {
            field.set(this, value);
        }else {
            throw new IllegalAccessException();
        }
    }
}
