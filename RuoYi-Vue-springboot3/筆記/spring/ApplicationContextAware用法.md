> 当一个类实现了这个接口之后，这个类就可以方便的获得 ApplicationContext对象（spring上下文）



Spring 发现某个 Bean 实现了 ApplicationContextAware 接口，Spring 容器会在创建该 Bean 之后，自动调用该 Bean 的 setApplicationContext（参数）方法，调用该方法时，会将容器本身 ApplicationContext 对象作为参数传递给该方法。

```java
// 全局上下文
@Component
public class ApplicationContextUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getApplicationContext(){
        return context;
    }

    /**
     * 通过name获取 Bean
     * @param name beanName
     * @return Object
     */
    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }

    public static <T> T getBean(Class<T> requiredType) throws BeansException{
        return getApplicationContext().getBean(requiredType);
    }
}


```
