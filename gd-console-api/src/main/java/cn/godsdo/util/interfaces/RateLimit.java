package cn.godsdo.util.interfaces;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author : yang
 * @Date : 2023/7/28
 * @ApiNote : 令牌桶
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int value() default 10; // 每秒钟允许的请求数
}
