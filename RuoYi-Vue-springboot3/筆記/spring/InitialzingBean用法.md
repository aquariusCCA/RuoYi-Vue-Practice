当一个类实现这个接口之后，Spring 启动后，**初始化** Bean 时，若该 Bean 实现 InitialzingBean 接口，会自动调用 afterPropertiesSet() 方法，完成一些用户自定义的初始化操作。

```java
public class SpringBeanInit implements InitializingBean {
 
    private Integer id;
 
    private String name;
 
    private Integer age;
 
    private boolean sex;
 
    private Student student;
 
    // 这里进行优先调用初始化一些参数
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("this is bean init set student data");
        Student student = new Student(id,name,age,sex);
        this.student = student;
    }
 
    public void testInit(){
        System.out.println("this is bean web.xml init-method invock");
    }
 
    public Student getStudent() {
        return student;
    }
 
    public void setStudent(Student student) {
        this.student = student;
    }
 
    public Integer getId() {
        return id;
    }
 
    public void setId(Integer id) {
        this.id = id;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public Integer getAge() {
        return age;
    }
 
    public void setAge(Integer age) {
        this.age = age;
    }
 
    public boolean isSex() {
        return sex;
    }
 
    public void setSex(boolean sex) {
        this.sex = sex;
    }
}

```

同样配置 Bean 的时候使用 **init-method** 也可以实现类似的操作

```xml
<bean id = "springBeanInit02" class = "com.lyj.studySpringBoot.init.SpringBeanInit" init-method="testInit">
    <property name="id" value="#{1111111}" />
    <property name="name" value="${test.springEL}" />
    <property name="age" value="#{10+8}" /> // SpringEL表达式
    <property name="sex" value="false" />
</bean>
```

在 spring 初始化 bean 的时候，如果该 bean 是实现了 InitializingBean 接口，并且同时 **配置文件** 中指定了 **init-method**，系统则是先调用 afterPropertiesSet 方法，然后在调用 init-method 中指定的方法。
