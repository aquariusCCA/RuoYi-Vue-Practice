> **參考文章：**
> - [同一个类中被调用，AOP可能不会生效](https://blog.csdn.net/Joash_Wu/article/details/141396403 "同一个类中被调用，AOP可能不会生效")
> - [Spring AOP 方法内部调用不生效](https://blog.csdn.net/kora1024/article/details/144963254 "Spring AOP 方法内部调用不生效")

# 原理

这句话的意思是，当你在一个类的内部调用同一个类中的方法时，`Spring AOP` 可能不会生效。

这是因为 `Spring AOP` 通过代理对象来实现，而当你在类内部调用方法时，实际上是通过 `this` 引用直接调用的，而不是通过代理对象调用的。

由于代理对象负责拦截方法调用并应用切面逻辑，所以直接调用会绕过 `AOP` 的拦截机制。

> 只有「經過代理」的呼叫才會觸發 AOP；類內部的 this 呼叫繞過代理，因此不會觸發 AOP。

正常外部方法调用，是基于 `AOP` 生成的 代理对象 进行的调用；本类调用，是 `this` 目标对象 直接调用，并不是代理对象进行调用

```java
@Slf4j
@Component
public class CServiceImpl implements CService {

    @Autowired
    private ApplicationContext applicationContext;

   @Override
    public String getValue(Request request) {
        // 此时切面注解不生效
        String value = this.getByCache(request.getId());
        return value;
    }

    @RedisCache
    public String getByCache(Long id){
        return "result";
    }
}
```

# 方法一：使用 `ApplicationContext` 获取自己
```java
@Slf4j
@Component
public class CServiceImpl implements CService {

    @Autowired
    private ApplicationContext applicationContext;

   @Override
    public String getValue(Request request) {
        // 自己获取自己，spring可以解决循环依赖
        CService bean = applicationContext.getBean(CServiceImpl.class);
        String value = bean.getByCache(request.getId());
        return value;
    }

    @RedisCache
    public String getByCache(Long id){
        return "result";
    }
}
```

# 方法二：自己注入自己
```java
@Slf4j
@Component
public class CServiceImpl implements CService {

    @Autowired
    private CService cservice;

    @Override
    public String getValue(Request request) {
        String value = cservice.getByCache(request.getId());
        return value;
    }

    @RedisCache
    public String getByCache(Long id){
        return "result";
    }
}
```