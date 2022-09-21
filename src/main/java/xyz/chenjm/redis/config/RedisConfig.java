package xyz.chenjm.redis.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public interface RedisConfig {
    default void initFromSource(PropertySource source) {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (Modifier.isFinal(mod) || Modifier.isPrivate(mod) || Modifier.isStatic(mod))
                continue;
            field.setAccessible(true);
            try {
                setField(field, source);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
    default void setField(Field field, PropertySource source) throws IllegalAccessException {
        String value = source.getPropertyIgnoreCase(field.getName());
        if (value == null)
            return;
        Class<?> type = field.getType();

        if (type == String.class) {
            field.set(this, value);
        } else if (type == Long.class || type == Long.TYPE) {
            field.setLong(this, Long.parseLong(value));
        } else if (type == Integer.class || type == Integer.TYPE) {
            field.setInt(this, Integer.parseInt(value));
        } else if (type == Double.class || type == Double.TYPE) {
            field.setDouble(this, Double.parseDouble(value));
        } else if (type == Float.class || type == Float.TYPE) {
            field.setFloat(this, Float.parseFloat(value));
        } else if (type == Boolean.class || type == Boolean.TYPE) {
            field.setBoolean(this, Boolean.parseBoolean(value));
        } else if (type == Byte.class || type == Byte.TYPE) {
            field.setByte(this, Byte.parseByte(value));
        } else {
            throw new IllegalAccessException();
        }
    }
}
