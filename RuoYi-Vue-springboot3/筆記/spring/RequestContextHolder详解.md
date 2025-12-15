> **參考文章:**
> - [RequestContextHolder详解（获取request对象的四种方法）](https://www.cnblogs.com/zwh0910/p/17168833.html "RequestContextHolder详解（获取request对象的四种方法）")
> - [SpringBoot--解决子线程无法获得HttpServletRequest的attribute的问题](https://blog.csdn.net/feiying0canglang/article/details/126646326 "SpringBoot--解决子线程无法获得HttpServletRequest的attribute的问题")
> - [RequestContextHolder和HttpServletRequest有什么区别？](https://blog.csdn.net/leavejimmyalone/article/details/133172485 "RequestContextHolder和HttpServletRequest有什么区别？")

# 获取request对象的四种方法
## 方法1、Controller中加参数来获取request
注意：只能在Controller中加入request参数。

一般，我们在Controller中加参数获取HttpServletRequest，如下所示：

```java
@RestController
@RequestMapping("/gap")
public class PlantTraceController {
    @PostMapping("/plantTrace")
    public Result2 savePlantTraceInfo(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        String methodName = request.getHeader("methodName");
        ....
}
```

该方法实现的原理是，在 Controller 方法开始处理请求时，Spring 会将 request 对象赋值到方法参数中。此时 request 对象是方法参数，相当于局部变量，毫无疑问是线程安全的。

Controller 中获取 request 对象后，如果要在其他方法中（如service方法、工具类方法等）使用request对象，需要在调用这些方法时将request对象作为参数传入。

### 优缺点
这种方法的主要缺点是 request 对象写起来冗余太多，主要体现在两点：

1. 如果多个 controller 方法中都需要 request 对象，那么在每个方法中都需要添加一遍 request 参数

2. request 对象的获取只能从 controller 开始，如果使用 request 对象的地方在函数调用层级比较深的地方，那么整个调用链上的所有方法都需要添加 request 参数

实际上，在整个请求处理的过程中，request 对象是贯穿始终的；也就是说，除了定时器等特殊情况，request 对象相当于线程内部的一个全局变量。而该方法，相当于将这个全局变量，传来传去。

## 方法2、自动注入来获取 request
注意：只能在Bean中注入request

```java
@Controller
public class TestController{
    @Autowired
    private HttpServletRequest request; //自动注入request
    @RequestMapping("/test")
    public void test() throws InterruptedException{
        //模拟程序执行了一段时间
        Thread.sleep(1000);
    }
}
```

### 优缺点
该方法的主要优点：

1. 注入不局限于 Controller 中：在方法1中，只能在 Controller 中加入 request 参数。而对于方法2，不仅可以在 Controller 中注入，还可以在任何 Bean 中注入，包括 Service、Repository 及普通的 Bean。

2. 注入的对象不限于 request：除了注入 request 对象，该方法还可以注入其他 scope 为 request 或 session 的对象，如 response 对象、session 对象等；并保证线程安全。

3. 减少代码冗余：只需要在需要 request 对象的 Bean 中注入 request 对象，便可以在该 Bean 的各个方法中使用，与方法1相比大大减少了代码冗余。

但是，该方法也会存在代码冗余。考虑这样的场景：web系统中有很多 controller，每个 controller 中都会使用 request 对象（这种场景实际上非常频繁），这时就需要写很多次注入 request 的代码；如果还需要注入 response，代码就更繁琐了。下面说明自动注入方法的改进方法，并分析其线程安全性及优缺点。

## 方法3：基类中自动注入（推荐）
注意：只能在 Bean 中注入 request

与方法2相比，将注入部分代码放入到了基类中。

基类代码：

```java
public class BaseController {
    @Autowired
    protected HttpServletRequest request;     
}
```

### 优缺点
与方法2相比，避免了在不同的 Controller 中重复注入 request；但是考虑到 java 只允许继承一个基类，所以如果 Controller 需要继承其他类时，该方法便不再好用。

无论是方法2和方法3，都只能在 Bean 中注入 request；如果其他方法（如工具类中static方法）需要使用request对象，则需要在调用这些方法时将 request 参数传递进去。下面介绍的方法4，则可以直接在诸如工具类中的 static 方法中使用 request 对象（当然在各种Bean中也可以使用）。

## 方法4：从RequestContextHolder中获取request
代码示例

```java
@Controller
public class TestController {
    @RequestMapping("/test")
    public void test() throws InterruptedException {
		ServletRequestAttributes servletRequestAttributes = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes()));
		//获取到Request对象
		HttpServletRequest request = servletRequestAttributes.getRequest();
		//获取到Response对象
		HttpServletResponse response = servletRequestAttributes.getResponse();
		//获取到Session对象
		HttpSession session = request.getSession();
        // 模拟程序执行了一段时间
        Thread.sleep(1000);
    }
}
```

### 优缺点
- 优点：可以在非 Bean 中直接获取。
- 缺点：如果使用的地方较多，代码非常繁琐；因此可以与其他方法配合使用。

# RequestContextHolder 和 HttpServletRequest 有什么区别？
RequestContextHolder 和 HttpServletRequest 都与 HTTP 请求相关，但它们的用途和工作方式有所不同。以下是它们之间的主要区别：

### HttpServletRequest:
- HttpServletRequest 是 Servlet API 的一部分，代表一个 HTTP 请求。

- 它提供了访问请求相关信息的方法，如请求参数、请求头、请求方法、请求 URI 等。

- 在 Spring 的 Controller 方法中，你可以直接将 HttpServletRequest 作为参数，Spring 会自动为你提供当前的请求对象。

### RequestContextHolder:

- RequestContextHolder 是 Spring 提供的一个工具类，用于存储和访问与当前请求相关的上下文信息。

- 它使用 ThreadLocal 来存储请求上下文，这意味着在同一个线程中，无论你在哪里，都可以使用 RequestContextHolder 访问与当前请求相关的上下文。

- 通过 RequestContextHolder，你可以获取到 ServletRequestAttributes，进而获取到 HttpServletRequest 和 HttpServletResponse。

- 它特别有用在那些不能直接接收 HttpServletRequest 作为参数的地方，例如在 Service 层、工具类或 AOP 切面中。