package com.ruoyi.framework.aspectj;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;

/**
 * 限流处理
 *
 * @author ruoyi
 *
 * NOTE: /筆記/ruoyi/限流.md
 */
@Aspect
@Component
public class RateLimiterAspect
{
    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    private RedisTemplate<Object, Object> redisTemplate;

    private RedisScript<Long> limitScript;

    @Autowired
    public void setRedisTemplate1(RedisTemplate<Object, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setLimitScript(RedisScript<Long> limitScript)
    {
        this.limitScript = limitScript;
    }

    // 在有注解@RateLimiter的方法前执行
    // 既然是实现接口限流功能肯定是要在切点前面行（也就是在接口执行之前），所以使用前置通知@Before。
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable
    {
        // 获取注解的限流时间
        int time = rateLimiter.time();
        // 获取注解的限流数量
        int count = rateLimiter.count();

        // 获取组合的键
        String combineKey = getCombineKey(rateLimiter, point);
        // 将键存入列表
        List<Object> keys = Collections.singletonList(combineKey);
        try
        {
            // 执行Redis脚本，获取限流结果
            // 通过执行 lua 脚本获取接口在限流时间内的执行次数，如果超过了限流次数就抛出异常限制接口的调用。
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            // 如果结果为空或者超过限制次数
            if (StringUtils.isNull(number) || number.intValue() > count)
            {
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
            log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), combineKey);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
    {
        // 创建一个StringBuilder对象，并初始化为RateLimiter的key
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
        // 如果RateLimiter的限制类型是IP
        if (rateLimiter.limitType() == LimitType.IP)
        {
            // 获取当前请求的IP地址，并追加到StringBuilder对象后，再追加一个"-"
            stringBuffer.append(IpUtils.getIpAddr()).append("-");
        }
        // 获取JoinPoint的MethodSignature对象
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 获取MethodSignature对象的方法对象
        Method method = signature.getMethod();
        // 获取方法的声明类
        Class<?> targetClass = method.getDeclaringClass();
        // 将方法的声明类和方法的名称追加到StringBuilder对象后，并在它们之间追加一个"-"
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        // 返回StringBuilder对象的字符串表示
        return stringBuffer.toString();
    }
}
