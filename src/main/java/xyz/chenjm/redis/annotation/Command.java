package xyz.chenjm.redis.annotation;

import java.lang.annotation.*;

/**
  注册为RedisCommand
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {
    String value() default "";
    boolean readonly() default true;

    /**
     * multi queue
     */
    boolean multi() default true;

    int args() default -1;
}
