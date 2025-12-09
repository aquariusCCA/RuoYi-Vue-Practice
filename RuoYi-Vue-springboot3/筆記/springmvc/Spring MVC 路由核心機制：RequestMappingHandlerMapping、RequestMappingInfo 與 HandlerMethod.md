# 一、整體關係先搞清楚

Spring MVC 收到一個 HTTP 請求時，核心流程可以簡化為：

1. `DispatcherServlet` 收到請求
2. 透過一堆 `HandlerMapping` 尋找「哪個 handler 要處理」

    * 其中負責註解式 Controller (`@Controller`, `@RestController`) 的，就是
      **`RequestMappingHandlerMapping`**
3. 找到後會回傳一個 **`HandlerMethod`**（包著 Controller 上的一個方法）
4. 再交給 `HandlerAdapter` 去呼叫該 `HandlerMethod`，做參數綁定、回傳處理等
5. 路由條件（路徑、HTTP 方法等）則由 **`RequestMappingInfo`** 描述

所以這三個東西可以簡單記成：

> `RequestMappingHandlerMapping`
> 　內部維護：`Map<RequestMappingInfo, HandlerMethod>`

---

# 二、RequestMappingHandlerMapping

### 1. 定義與角色

* 所屬套件：

    * `org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping`
* 身分：

    * 一個 `HandlerMapping` 的實作
    * 負責「註解式 Controller」的路由：

        * `@RequestMapping`
        * `@GetMapping`
        * `@PostMapping`
        * `@PutMapping`
        * `@DeleteMapping`
        * `@PatchMapping` 等

**一句話：**

> 它是「註解式路由的管理者」，負責建立、維護、查詢「請求條件 ➜ Controller 方法」的對應關係。

### 2. 啟動階段的工作（建表）

在 Spring Boot 啟動時，它會做：

1. 掃描所有 Spring Bean，尤其是 `@Controller` / `@RestController`
2. 找出有 `@RequestMapping` / `@*Mapping` 的方法
3. 將這些方法解析成：

    * 一個 `RequestMappingInfo`（路由條件）
    * 一個 `HandlerMethod`（要呼叫的方法）
4. 存進內部的 Map：

    * `Map<RequestMappingInfo, HandlerMethod> handlerMethods`

你現在的這段程式碼：

```java
RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
```

拿到的就是這張 **完整路由表**。

### 3. 請求階段的工作（查表）

每次有 HTTP 請求進來時：

1. `DispatcherServlet` 依序問各個 `HandlerMapping`：

   > 這個請求（URL + Method + Header …）誰要處理？
2. `RequestMappingHandlerMapping`：

    * 把請求轉成一個「條件物件」
    * 拿來跟所有的 `RequestMappingInfo` 比對（路徑、方法、consumes/produces、params、headers 條件…）
    * 找到最匹配的那一個
    * 回傳對應的 `HandlerMethod`

---

# 三、RequestMappingInfo

### 1. 定義與角色

* 所屬套件：

    * `org.springframework.web.servlet.mvc.method.RequestMappingInfo`
* 身分：

    * 是 `Map<RequestMappingInfo, HandlerMethod>` 的 **key**
    * 用來精準描述「這個路由適用於什麼請求條件？」

可以把它想成**一個 Endpoint 的「條件組合」**。

### 2. 主要包含的資訊

典型會包含：

* **路徑 pattern：**

    * 例：`/users`, `/users/{id}`, `/api/v1/orders/**`
* **HTTP 方法：**

    * GET / POST / PUT / DELETE / PATCH…
* **consumes：**

    * 請求的 Content-Type 限制，例：`application/json`
* **produces：**

    * 回應的 Media-Type，例：`application/json`, `text/html`
* **params 條件：**

    * 例：`params = "version=1"`、`params = "!debug"` 等
* **headers 條件：**

    * 例：`headers = "X-Requested-With=XMLHttpRequest"`
* **自訂 condition：**

    * 例如 WebFlux 或某些進階擴充時可以帶自訂條件

這些都來自你在 Controller 上標的註解，例如：

```java
@GetMapping(
    value = "/users/{id}",
    produces = "application/json",
    params = "version=1"
)
public User getUserV1(@PathVariable Long id) { ... }
```

Spring 啟動時會把這些註解解析成一個 `RequestMappingInfo` 實例。

### 3. 存在的意義

* 讓 Spring 可以：

    * **精準比對請求**（不只看 URL，還看 method、header、params…）
    * 處理多個 mapping 之間的「優先順序」、「衝突」、「模糊匹配」等問題
* 也是你現在列印 map 時看到的那一段 `RequestMappingInfo: ...` 字串

---

# 四、HandlerMethod

### 1. 定義與角色

* 所屬套件：

    * `org.springframework.web.method.HandlerMethod`
* 身分：

    * `Map<RequestMappingInfo, HandlerMethod>` 的 **value**
    * 包裝「真正要被呼叫的 Controller 方法」

一句話：

> `HandlerMethod` = 「某個 bean（Controller 實例）上的某一個 method」＋一堆反射相關資訊。

### 2. 主要包含的資訊

* **bean相關：**

    * `bean`：實際的 Controller 實例（或其 bean 名稱）
    * `beanType`：Controller 類別，例如 `com.example.UserController`
* **方法相關：**

    * `method`：`java.lang.reflect.Method` 物件
    * 方法名稱：例如 `getUser`
    * 參數型別：`Long id`, `HttpServletRequest request`, `@RequestBody UserDto dto`…
    * 回傳型別：例如 `User`, `ResponseEntity<User>`, `String`…
* **註解相關：**

    * 方法上的註解，包含：

        * `@RequestMapping` / `@GetMapping` / `@PostMapping`…
        * `@ResponseBody`、`@Transactional` 等
* **參數/回傳的額外資訊：**

    * 例如哪些參數有 `@PathVariable`、`@RequestParam`、`@RequestBody` 等

### 3. 請求處理時怎麼被用到

請求匹配到某個 `HandlerMethod` 之後：

1. 交給 `RequestMappingHandlerAdapter`
2. `HandlerAdapter` 根據 `HandlerMethod`：

    * 用一堆 `HandlerMethodArgumentResolver` 幫你解析參數

        * PathVariable、RequestParam、RequestBody、Session、Header…
    * 最後用反射呼叫 `method.invoke(bean, args...)`
3. 把回傳值交給對應的 `HandlerMethodReturnValueHandler` 處理

    * 包成 `ModelAndView` 或寫入 response body

因此：

> `HandlerMethod` 是 Spring MVC 在「方法層級」運作的核心單位。

---

# 五、三者之間的關係（總整理）

可以畫成這樣的心智圖：

1. **啟動階段：建表**

    * `RequestMappingHandlerMapping` 掃描所有 Controller
    * 對每個 `@*Mapping` 方法：

        * 產生一個 `RequestMappingInfo`（描述條件）
        * 產生一個 `HandlerMethod`（包著實際要呼叫的方法）
    * 存進：

        * `Map<RequestMappingInfo, HandlerMethod>`

2. **請求階段：查表 + 執行**

    * `DispatcherServlet` 收到 HTTP 請求
    * 呼叫 `RequestMappingHandlerMapping.getHandler(request)`

        * 利用 `RequestMappingInfo` 進行條件比對
        * 找到對應的 `HandlerMethod`
    * 把 `HandlerMethod` 交給 `HandlerAdapter` 執行

---

# 六、給你一份可以直接抄的筆記摘要

你可以整理成類似這樣（示意）：

```text
[RequestMappingHandlerMapping]
- HandlerMapping 的實作，負責註解式 Controller 的路由管理
- 啟動時：
  - 掃描 @Controller / @RestController
  - 找出有 @RequestMapping / @GetMapping 等的方法
  - 建立 Map<RequestMappingInfo, HandlerMethod>
- 請求時：
  - 根據請求(URL + HTTP Method + headers + params + consumes/produces)
  - 在所有 RequestMappingInfo 中比對，找到對應的 HandlerMethod
- 本質：路由表的「管理者」(建表 + 查表)

[RequestMappingInfo]
- Map 的 key，描述一個路由的「匹配條件」
- 內容包含：
  - 路徑 patterns: /users, /users/{id}, /api/** 等
  - HTTP methods: GET / POST / PUT / DELETE / PATCH
  - consumes / produces
  - params / headers 條件
  - 其他自訂條件
- 由 @RequestMapping / @GetMapping 等註解解析而來
- 用途：
  - 精準匹配請求
  - 解決多個 mapping 的優先順序與衝突

[HandlerMethod]
- Map 的 value，代表「要被呼叫的 Controller 方法」
- 內容包含：
  - bean / beanType：哪個 Controller 實例
  - method：java.lang.reflect.Method
  - 方法參數型別、回傳型別
  - 方法上的註解資訊
- 請求處理流程：
  - 被 HandlerMapping 找到並回傳
  - 交由 HandlerAdapter 透過反射呼叫
  - 配合 ArgumentResolver / ReturnValueHandler 完成參數綁定與回傳處理
```