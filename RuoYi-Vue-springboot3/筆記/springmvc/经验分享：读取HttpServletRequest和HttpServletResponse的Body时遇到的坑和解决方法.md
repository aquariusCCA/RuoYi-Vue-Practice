> **參考文章：** 
> [经验分享：读取HttpServletRequest和HttpServletResponse的Body时遇到的坑和解决方法](https://zhuanlan.zhihu.com/p/647912672 "经验分享：读取HttpServletRequest和HttpServletResponse的Body时遇到的坑和解决方法")

最近为了方便排查问题，打算给一个Spring Boot项目加上访问日志，记录每次请求的原始请求体和返回体。本以为这是个简单的任务，应该在几分钟内就可以完成。结果在实现过程中过程中遇到好多问题，最终花费了好几个小时。实际上，不仅仅是日志记录的需要，有些API也需要对原始请求体进行签名校验。因此，这也是一个比较常见的需求。

这篇文章会记录整个实现和思考过程，同时详细说明遇到的问题和各种解决方案及其局限。不想看过程的读者可以快进到小结部分。

基本思路很简单，编写个 Filter，在 HTTP 请求进来时先读取请求体，然后在返回之前再读取返回体。我们首先从 HttpServletRequest 开始。

# 从 HttpServletRequest 读取请求体

在查找 `HttpServletRequest` 接口的定义时，发现有个方法 `getInputStream`，它可以用于读取请求体的二进制数据。果断写段代码测试下：

```java
/* 注意，非正确用法 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // 一行读取body，看起来很简单，也成功打印出请求体了
        var reqBody = new String(request.getInputStream().readAllBytes());
        log.info("Raw Request Body: {}", reqBody);
        filterChain.doFilter(request, response);
    }
}
```

发送一个 POST 请求，结果返回可 500 错误，赶紧查看日志，发现读取请求体没问题，正常地打印出来了。到底哪里出问题了呢？继续翻看日志，发现有一条错误信息：

```shell
org.springframework.http.converter.HttpMessageNotReadableException: Required request body is missing
```

request body 怎么就 missing 了？原来 `InputStream` 是一个数据流，它只能被读取一次。我们在请求一进来就读取了 `InputStream`，导致后续再读取的时候，读到的就是空的。
 
看来这个问题没这么简单。于是上网查了一些资料，发现 Spring 有一个记录请求日志的 `Filter` 实现叫做 `CommonsRequestLoggingFilter`，可惜它的日志格式是固定的，无法自定义。

但是没关系，我们可以借鉴它的实现方式方法。在分析了它的源码后，找到了一个可以帮助我们解决问题的核心类，叫做 `ContentCachingRequestWrapper`。

# ContentCachingRequestWrapper

`ContentCachingRequestWrapper` 的核心思想是将请求体缓存起来，从而可以多次读取。

它继承了 `HttpServletRequestWrapper`，本身就可以当成正常的 `HttpServletRequest` 使用。

它提供了一个特殊的方法 `getContentAsByteArray` 用于多次读取 body。

我们修改下代码，用 `ContentCachingRequestWrapper` 代替原始的 `HttpServletRequest`：

```java
/* 注意，非正确用法 */
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    // 1. 用ContentCachingRequestWrapper包装原始的request
    var requestToUse = new ContentCachingRequestWrapper(request);
    // 2. 通过特殊方法来获取body
    var reqBody = new String(requestToUse.getContentAsByteArray());
    log.info("Raw Request Body: {}", reqBody);
    // 3. 后续都要用ContentCachingRequestWrapper，而不是原始的request了
    filterChain.doFilter(requestToUse, response);
}
```

再次发送一个 POST 请求，返回结果正常了，但是为什么请求体没有打印出来？仔细看了下方法 `getContentAsByteArray` 的说明，

发现有一句”If the application does not read the content, this method returns an empty array“。再配合源码分析，原来只有从流里读取过数据后，它才会把数据缓存起来。

我们尝试手动读取一下数据流呢，但事实证明不行，依然会导致后续读不到 Body。

原因是，`ContentCachingRequestWrapper` 底层还是只能读取一次的数据流，它只是将读取到的数据缓存起来而已，读取过一次之后这个流本身就空了，后续再读数据只能通过 `getContentAsByteArray` 从缓存中读取。

不过，这里有一个折中的解决方案，就是将读取的时机改到 `filterChain.doFilter` 之后，最终的代码如下：

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    // 1. 不支持异步Dispatch模式，同时防止多次嵌套的意外情况
    boolean isFirstRequest = !isAsyncDispatch(request);
    HttpServletRequest requestToUse = request;
    if (isFirstRequest 
        && !(request instanceof ContentCachingRequestWrapper)) {
        requestToUse = new ContentCachingRequestWrapper(request);
    }

    // 2. 后续依然要用wrapper过的对象
    filterChain.doFilter(requestToUse, response);

    // 3. 把读取请求body放到最后
    String reqBody = "";
    if (requestToUse instanceof ContentCachingRequestWrapper) {
        reqBody = new String(((ContentCachingRequestWrapper) requestToUse).getContentAsByteArray());
    }
    log.info("Raw Request Body: {}", reqBody);
}
```

除了读取时机的问题外，还有一个小瑕疵，就是如果 `Controller` 没有读取请求体的场景（比如代码中根本用不到请求体），不管这个 Body 内容有没有，`getContentAsByteArray` 最后也是读取不到数据的。

不过这点应该还可以接受，毕竟Body用不到，记录的意义也不大。

类似的，你也可以在Controller层获取原始Body，用于对其进行签名校验。

```java
@RequestMapping(value = "/signBody", method = {RequestMethod.POST})
public RespDTO<String> param(
        // 注意，一定要有这行使用到Body的代码，不然后面会读取不到缓存的body内容
        @RequestBody TestBodyData data,
        HttpServletRequest request){
    if (request instanceof ContentCachingRequestWrapper) {
        // 读取缓存中的body内容
        var reqBody = new String(((ContentCachingRequestWrapper) request).getContentAsByteArray());
        log.info("Get Raw Body From Controller: {}", reqBody);
        String checksum = sign(reqBody);
        return RespDTO.ok()
    }
    return RespDTO.error();
}
```

如果我们一定要在开头就读取到请求体，有什么办法吗？当然有的，我们可以借鉴 `ContentCachingRequestWrapper` 的实现，自己实现一个缓存 Wrapper。

# 自定义缓存ServletRequestWrapper

`ContentCachingRequestWrapper` 的底层依然是委托给原始的 InputStream，因此还是只能读取一次，重复读取必须用特殊的方法从缓存中读取。

我们可以换个思路，一开始就把 `InputStream` 的内容读取出来，然后缓存到一个字节数组中，然后创建自定义的 InputStream，所有对 InputStream 的操作都改成对这个字节数组的操作中。

首先，我们创建一个底层为字节数组的 InputStream，取名为 `CachingInputStream`,，源码如下：

```java
public class CachingInputStream extends ServletInputStream {
    // 底层是个字节数组
    final ByteArrayInputStream byteArrayInputStream;

    public CachingInputStream(byte[] requestBodyBytes) {
        // 实例化的时候就把读取到的字节缓存起来
        this.byteArrayInputStream = 
            new ByteArrayInputStream(requestBodyBytes);
    }

    /* 下面的都是ServletInputStream的必要重写方法 */
    @Override
    public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        // Not implemented
    }

    @Override
    public int read() throws IOException {
        // 直接从缓存读取
        return byteArrayInputStream.read();
    }
}
```

然后我们写一个继承自 `HttpServletRequestWrapper` 的缓存 `Wrapper` 类 `CachingRequestWrapper`，源码如下：

```java
public class CachingRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] requestBodyBytes;

    public CachingRequestWrapper(HttpServletRequest request) 
        throws IOException {
        super(request);
        // 实例化就读取body并缓存起来
        requestBodyBytes = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 每次读取都返回一个新的InputStream，底层数据是缓存的Body
        return new CachingInputStream(requestBodyBytes);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // 每次读取都返回一个新的BufferedReader，底层数据是缓存的Body
        return new BufferedReader(
            new InputStreamReader(
                getInputStream(), getCharacterEncoding()));
    }

    /* 两个读取body的辅助方法 */
    public byte[] getRequestBodyBytes() {
        return requestBodyBytes;
    }

    public String getRequestBodyString() {
        return new String(requestBodyBytes);
    }
}
```

这个 `CachingRequestWrapper` 的用法跟 `Spring` 的 `ContentCachingRequestWrapper` 基本相同，这里就不重复了。

那么问题来了，为什么 Spring 不用这种方式呢？这样用起来会更加方便。

个人认为有两个主要原因。首先，这种方式存在内存占用问题。我们的自定义 Wrapper 一上来就把整个请求缓存起来，而且一直持续到请求处理完成。

对于请求体较大的情况，会对系统的内存造成一定的压力，因此一定要设置一个阈值，超过这个阈值时应降级，避免缓存过大的请求体。

其次，我们完全自定义了一个 `ServletInputStream`，其行为和支持的功能跟原始的 `ServletInputStream` 不同，而且也不支持异步的Dispatch类型。

虽然我们的应用可以承受这种不一致带来的风险，但是作为一个框架就不能这么做。

因此，个人不太建议使用这种方式的，更倾向于采用 `ContentCachingRequestWrapper` 的方案。如果非要用这种方案，务必限制请求大小，并添加黑白名单功能，以尽量减小影响范围。

至此，关于请求体的问题差不多解决了，那么返回结果呢？

# 读取返回体：ContentCachingResponseWrapper

`HttpServletResponse` 和 `HttpServletRequest` 类似，它的返回体也位于一个数据流中，因此也只能读取一次。

有了前面的经验，很快就找到了返回体对应的 `Wrapper` 类 `ContentCachingResponseWrapper`。

它的原理很简单，当调用相应的方法往输出流里写数据的时候，它会将数据缓存起来。看似很简单，但是使用起来还是有两个坑要注意：

1. 它仅仅将写入的数据缓存起来了，而没有同时写到 `OutputStream` 中。这导致返回结果一直为空。解决方法是，在最后一定要手动调用它的 `copyBodyToResponse` 方法，将缓存的数据写入输出流里。

2. 调用完 `copyBodyToResponse` 后，缓存又被清空了，因此 `getContentAsByteArray` 一定要在之前调用，才能获取到缓存的返回体。

以下是正确的示例代码：

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    // 1. 用ContentCachingResponseWrapper包装原始的response
    ContentCachingResponseWrapper responseWrapper = 
        new ContentCachingResponseWrapper(response);

    // 2. 后续都要用这个wrapper
    filterChain.doFilter(request, responseWrapper);

    // 3. 要在copyBodyToResponse调用之前，否则缓存会被清空
    String respBody = new String(responseWrapper.getContentAsByteArray());
    if (request.isAsyncStarted()) { // 异步场景的支持
        request.getAsyncContext().addListener(new AsyncListener() {
            public void onComplete(AsyncEvent asyncEvent) throws IOException {
                // 4. 注意返回之前一定要调用这行代码，将缓存写入输出流
                responseWrapper.copyBodyToResponse();
            }

            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            }

            public void onError(AsyncEvent asyncEvent) throws IOException {
            }

            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
            }
        });
    } else {
        // 4. 注意返回之前一定要调用这行代码，将缓存写入输出流
        responseWrapper.copyBodyToResponse();
    }
    log.info("Raw Response: {}", respBody);
}
```