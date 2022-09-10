package xyz.chenjm.redis.annotation;

import java.lang.annotation.*;

/**
 * 添加文件源配置
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Source {
    String value();
}
