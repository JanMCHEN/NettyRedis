package annotation;

import java.lang.annotation.*;

/**
  注册为RedisCommand；
  value: RedisCommand的key
  scope: RedisCommand的作用域
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {
    String value();
    String scope() default "all";

}
