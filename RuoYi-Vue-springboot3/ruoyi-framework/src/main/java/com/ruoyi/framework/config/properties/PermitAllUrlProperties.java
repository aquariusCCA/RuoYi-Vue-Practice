package com.ruoyi.framework.config.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.ruoyi.common.annotation.Anonymous;

/**
 * 设置Anonymous注解允许匿名访问的url
 * 项目启动的时候，会将Anonymous注解的路径提取出来，放到urls list里面
 *
 * @author ruoyi
 *
 * NOTE: RuoYi-Vue-springboot3/筆記/ruoyi/若依的@Anonymous注解.md
 * NOTE: RuoYi-Vue-springboot3/筆記/spring/ApplicationContextAware用法.md
 * NOTE: RuoYi-Vue-springboot3/筆記/spring/InitialzingBean用法.md
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
        /**
         * RequestMappingHandlerMapping 本身比較像是：
         * 啟動時：
         *      掃描所有 @Controller / @RestController
         *      找出所有有 @RequestMapping / @GetMapping / @PostMapping… 的方法
         *      建好那張 Map<RequestMappingInfo, HandlerMethod>（路由表）
         * 請求進來時：
         *      依照 HTTP 請求資訊（URL、Method、Header、Content-Type…）在這張路由表裡找到正確的 HandlerMethod
         *
         * NOTE: /筆記/springmvc/Spring MVC 路由核心機制：RequestMappingHandlerMapping、RequestMappingInfo 與 HandlerMethod.md
         */
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);

        /**
         * Map<RequestMappingInfo, HandlerMethod> = 路由表
         * RequestMappingInfo = 路由條件，例如 /users/{id}
         * HandlerMethod = 要呼叫的 controller 方法
         *
         * NOTE: /筆記/springmvc/Spring MVC 路由核心機制：RequestMappingHandlerMapping、RequestMappingInfo 與 HandlerMethod.md
         */
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        // 可以打註解看一下輸出，就會明白
        // map.forEach((requestMappingInfo, handlerMethod) -> {
        //     // 遍歷每個 RequestMappingInfo 和對應的 HandlerMethod
        //     System.out.println("RequestMappingInfo: " + requestMappingInfo);
        //     System.out.println("HandlerMethod: " + handlerMethod);
        // });

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
