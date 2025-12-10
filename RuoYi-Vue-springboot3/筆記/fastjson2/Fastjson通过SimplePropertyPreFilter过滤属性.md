引入依賴：
```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>annotationProcessor</scope>
    </dependency>

    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.58</version>
    </dependency>
</dependencies>
```

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private int id;
    private String userName;
    private String password;
    private String sex;
    private String age;
    private Date birthday;
}
```

```java
@Test
public void test01() throws ParseException {
    User user=new User();
    user.setId(1);
    user.setUserName("admin");
    user.setPassword("123456");
    user.setSex("男");
    user.setAge("28");
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    user.setBirthday(format.parse("1997-02-10"));
    String userJSON= JSON.toJSONString(user);
    String userJSON2=JSON.toJSONString(user,new SimplePropertyPreFilter(User.class, "userName","sex"));
    SimplePropertyPreFilter simplePropertyPreFilter = new SimplePropertyPreFilter(User.class, "userName","sex");
    simplePropertyPreFilter.getExcludes().add("sex");//添加排除属性
    simplePropertyPreFilter.getIncludes().add("birthday");//添加属性
    String userJSON3=JSON.toJSONString(user,simplePropertyPreFilter);
    System.out.println("userJSON"+userJSON);
    System.out.println("userJSON2使用了SimplePropertyPreFilter过滤器--->"+userJSON2);
    System.out.println("userJSON3使用了SimplePropertyPreFilter过滤器添加或排除属性--->"+userJSON3);
}
```

运行结果：

```shell
userJSON{"age":"28","birthday":"1997-02-10 00:00:00","id":1,"password":"123456","sex":"男","userName":"admin"}
userJSON2使用了SimplePropertyPreFilter过滤器--->{"sex":"男","userName":"admin"}
userJSON3使用了SimplePropertyPreFilter过滤器添加或排除属性--->{"birthday":"1997-02-10 00:00:00","userName":"admin"}
```