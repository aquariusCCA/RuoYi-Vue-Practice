> **參考文章**:[笔记：若依的@Anonymous注解](https://blog.csdn.net/weixin_44522680/article/details/136407352)

# 定义@Anonymous

> 作用：允许匿名访问

```java
/**
 * 匿名访问不鉴权注解
 * 也就是说不管是类还是方法上面，只要有这个注解，不需要登录就能访问
 * @author ruoyi
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous
{
}

```

# 在不需要权限的接口上使用该注解

```java
@Anonymous
@GetMapping("test")
public AjaxResult test(){

}
```

# PermitAllUrlProperties类

```java
/**
 * 设置Anonymous注解允许匿名访问的url
 * 
 * @author ruoyi
 */
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    // 定义一个规则
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    // 设置上下文对象
    private ApplicationContext applicationContext;

    // url的list 设置Anonymous注解的url 都放到这个里面，也就是这个里面的路径，都是不需要权限就可以访问的
    private List<String> urls = new ArrayList<>();

    public String ASTERISK = "*";

    // 项目初始化的时候，就会走这里
    @Override
    public void afterPropertiesSet()
    {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);

        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        // info代表每一个url对象
        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            //  AnnotationUtils.findAnnotation()为第三方的依赖，进行判断 一个方法上有没有注解
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            // ifPresent()方法就是会返回一个boolean类型值，如果对象不为空则为真，如果为空则为false
            Optional.ofNullable(method).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues()) 
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;
    }

    public List<String> getUrls()
    {
        return urls;
    }

    public void setUrls(List<String> urls)
    {
        this.urls = urls;
    }
}

```

# 在springSecurity的配置类SecurityConfig中配置

```java
/**
 * spring security配置
 * 
 * @author shilin
 */
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig
{
 

    /**
     * 允许匿名访问的地址
     */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            // 省略其他 ...
            .authorizeHttpRequests((requests) -> {
                // 注解标记允许匿名访问的url
                permitAllUrl.getUrls().forEach(url -> requests.requestMatchers(url).permitAll());
                // 省略其他 ...
            })
            // 省略其他 ...
    }
}

```
