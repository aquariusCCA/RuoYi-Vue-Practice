> **參考文章：**
> - [东八区的 springboot 如何配置序列化](https://blog.csdn.net/HongZeng_CSDN/article/details/129977117 "东八区的 springboot 如何配置序列化")

# 使用SpringBoot默认配置
SpringBoot 默认使用 UTC 时间，如果我们需要使用东八区时间，可以使用以下配置：

```yaml
spring:
  jackson:
    time-zone: GMT+8
```

- 优点：配置简单，在代码中直接使用自动装配的组件即可

- 缺点：如果有多个模块需要使用不同的时区，需要在不同的模块中分别进行配置

> 这种方式是最简单的方式，不需要任何额外的依赖和代码，但是需要注意的是，该配置是全局生效的，可能会影响到其他需要使用 UTC 时间的地方，而且每次新增依赖、升级 SpringBoot 版本等情况都需要再次检查该配置是否正确。

# 自定义配置类

另外一种方式是自定义配置类，使用 `@Configuration` 注解创建一个配置类，然后在该类中配置 `Jackson2ObjectMapperBuilderCustomizer`，指定时区为东八区。

```java
@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.timeZone(TimeZone.getTimeZone("GMT+8"));
    }
}
```

> 这种方式需要自定义代码，但是可以更加灵活地控制使用东八区时间的范围，而且不会影响到其他需要使用 UTC 时间的地方。但是同样需要注意每次新增依赖、升级 SpringBoot 版本等情况都需要再次检查该配置是否正确。

# 自定义 ObjectMapper
也可以通过自定义 Jackson 的 ObjectMapper 来使用东八区时间。

具体实现方式是，在 ObjectMapper 上设置一个自定义的 JavaTimeModule，然后在该模块上设置时区为东八区。

示例代码如下：

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CustomObjectMapper extends ObjectMapper {
    public CustomObjectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        this.registerModule(javaTimeModule);
        this.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }
}
```

在上面的示例中，我们创建了一个继承自 ObjectMapper 的 CustomObjectMapper，并在该对象上注册了一个自定义的 JavaTimeModule，该模块的序列化和反序列化方式分别使用了 LocalDateTimeSerializer 和 LocalDateTimeDeserializer，同时将时区设置为 Asia/Shanghai。你也可以根据需要添加其他的时间序列化和反序列化方式。

在代码中使用自定义的 CustomObjectMapper 对象进行序列化和反序列化即可使用东八区时间。例如：

```java
CustomObjectMapper objectMapper = new CustomObjectMapper();
String jsonString = objectMapper.writeValueAsString(yourObject);
YourObject deserializedObject = objectMapper.readValue(jsonString, YourObject.class);
```

值得注意的是，如果你需要在 Spring Boot 中使用自定义的 ObjectMapper，则需要在配置类中进行相关配置：

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new CustomObjectMapper();
    }
}
```

> 这样配置之后，在代码中使用 @Autowired 注入该 ObjectMapper 对象即可

# 自定义序列化器

第三种方式是自定义序列化器，在序列化的过程中将时间转换为东八区时间。需要实现 `JsonSerializer` 接口，然后在 `@JsonSerialize` 注解中指定该序列化器。

具体代码如下：

```java
public class ChinaZoneDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.withZoneSameInstant(ZoneId.of("GMT+8")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
```

然后在需要序列化的对象的时间字段上使用 `@JsonSerialize` 注解，指定该序列化器。

```java
@JsonSerialize(using = ChinaZoneDateTimeSerializer.class)
private ZonedDateTime createTime;
```

> 这种方式可以更加灵活地控制时间的格式和转换逻辑，但是需要自定义代码，而且对每个需要转换的时间字段都需要添加 `@JsonSerialize` 注解，有一定的代码侵入性。