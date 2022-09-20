package xyz.chenjm.redis.annotation;

import java.lang.annotation.*;

/**
 * 扫描路径
 * value==basePackages
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandScan {
    String[] value() default {};
    String[] basePackages() default {};
}
