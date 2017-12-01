package cn.godsdo.util.interfaces;

/**
 * @Author : yang
 * @Date : 2021/7/28
 * @ApiNote :令牌桶限流注解
 */
import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Pointcut("@annotation(rateLimit)")
    public void rateLimitPointcut(RateLimit rateLimit) {
    }

    @Before("rateLimitPointcut(rateLimit)")
    public void rateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = joinPoint.getSignature().toLongString();
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.value()));

        if (!rateLimiter.tryAcquire()) {
            throw new RuntimeException("接口访问频率超限");
        }
    }
}
