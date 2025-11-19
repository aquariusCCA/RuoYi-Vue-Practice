````markdown
# Redis 序列化：Fastjson autoType + 白名單（以若依為例）

## 一、背景：為什麼若依要自訂 RedisSerializer？

Spring Data Redis 本身已提供多種序列化器，例如：

- `StringRedisSerializer`
- `JdkSerializationRedisSerializer`
- `Jackson2JsonRedisSerializer`
- `GenericJackson2JsonRedisSerializer`
- `GenericToStringSerializer` 等

**但沒有 Fastjson / Fastjson2 的官方實作**。  
若依想要的是「**Fastjson2 + 類型資訊 + 安全白名單**」，這種行為 Spring 沒有內建，所以只好自己寫一個 `RedisSerializer`。

---

## 二、若依的 FastJson2JsonRedisSerializer 做了什麼？

核心程式碼（簡化版）：

```java
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T>
{
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);

    private Class<T> clazz;

    public FastJson2JsonRedisSerializer(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException
    {
        if (t == null)
        {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName)
                   .getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length <= 0)
        {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);

        return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
    }
}
````

### 功能拆解

1. **使用 Fastjson2 作為 JSON 序列化工具**

    * 而不是 Spring 官方偏好的 Jackson。
    * 符合部分團隊／中國開源圈慣用技術選型。

2. **`WriteClassName`：在 JSON 中寫入類型資訊（多型支援）**

   ```java
   JSON.toJSONString(t, JSONWriter.Feature.WriteClassName);
   ```

   這會產生類似：

   ```json
   {
     "@type": "com.xxx.User",
     "id": 1,
     "name": "Tom"
   }
   ```

   作用：

    * JSON 裡記錄了實際類型（`@type`）。
    * 反序列化時，即使只傳 `Object.class`，仍可以還原成原本的具體類別（多型）。

3. **autoType + 白名單：防止反序列化攻擊**

   ```java
   static final Filter AUTO_TYPE_FILTER =
       JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);

   return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
   ```

    * 開啟 autoType：允許根據 JSON 的 `@type` 自動決定要 new 哪個類。
    * 加上白名單：**只允許白名單內指定的 package / 類被實例化**，其他一律拒絕。

---

## 三、什麼是 autoType？

### 1. 基本概念

autoType 本質上是：

> 根據 JSON 裡的 `@type` 字段，自動決定反序列化的具體類型。

例子：

```java
User u = new User(1L, "Tom");
String json = JSON.toJSONString(u, JSONWriter.Feature.WriteClassName);
```

產出的 JSON：

```json
{
  "@type": "com.example.User",
  "id": 1,
  "name": "Tom"
}
```

反序列化：

```java
Object obj = JSON.parseObject(json, Object.class); // 實際會還原成 User
```

因為 JSON 裡有 `@type: "com.example.User"`，
Fastjson 能自動 new 出 `com.example.User`，而不是單純 Map。

### 2. 與若依 Redis 配置的關係

若依在 Redis 中配置的 value 序列化：

```java
@Bean
@SuppressWarnings(value = { "unchecked", "rawtypes" })
public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
{
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    template.afterPropertiesSet();
    return template;
}
```

* `FastJson2JsonRedisSerializer` 是以 `Object.class` 建立。
* 但透過 `WriteClassName` 寫入 `@type`，因此實際可以還原成原來的具體類別。
* 支援：同一個 key / hash 裡可以存各種不同的類型，讀出來仍能正確還原。

---

## 四、為什麼 autoType 很危險？（需要白名單的原因）

只要滿足兩個條件：

1. 你有開 autoType（允許 `@type` 決定類型）
2. JSON 內容可被外部控制（HTTP API / MQ / Redis 可被注入等）

那攻擊者就可以傳：

```json
{
  "@type": "惡意或危險的類名",
  "payload": "..."
}
```

如果這個類在某些方法 / 反序列化過程中會執行系統指令、發 HTTP、讀檔寫檔等，就可能出現 **RCE（遠端程式執行）** 風險。

結論：

> 「允許外部任意指定 `@type` → 等於允許外部決定你要 new 哪個類 → 風險極大。」

Fastjson 歷史上已經有多次因 autoType 導致的安全漏洞，
Fastjson2 才強制把 autoType 設計為需要明確控制、搭配安全機制。

---

## 五、白名單 `JSONReader.autoTypeFilter(...)` 的角色

若依使用：

```java
static final Filter AUTO_TYPE_FILTER =
    JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);
```

概念是：

> 「我只允許某些 package / 類被 autoType，其他全部禁止。」

假設白名單是：

```java
public static final String[] JSON_WHITELIST_STR = new String[] {
    "com.ruoyi."
};
```

那麼只有 `com.ruoyi` 底下的類才允許被反序列化：

* `@type": "com.ruoyi.system.domain.SysUser"` ✅ 允許
* `@type": "java.lang.Runtime"` ❌ 拒絕
* `@type": "org.apache.commons.xxxxx"` ❌ 拒絕

配合 Redis 使用場景：

* 系統自己往 Redis 寫入資料（內部控制）
* 反序列化時，Fastjson 會根據 `@type` 還原，但只在白名單中挑類
* 例如：`SysUser`、`SysDept`、一些 VO、DTO 等，都是專案內的安全實體類

這樣：

* 保留了 autoType 的「多型與還原」優點
* 又透過白名單降低了被惡意利用的風險

---

## 六、與 Spring 官方 Jackson 序列化器的對比

### Spring `GenericJackson2JsonRedisSerializer`

* 使用 Jackson 作為 JSON 序列化工具。
* 也會寫入類型資訊（但用的是 Jackson 自己的方式，不是 `@type`）。
* 同樣支援多型與還原。
* 安全控制依賴 Jackson 的 `PolymorphicTypeValidator` 等機制。

### 若依自訂 `FastJson2JsonRedisSerializer`

* 使用 Fastjson2，而非 Jackson。
* 寫入類型資訊用 `WriteClassName` → JSON 裡有 `@type`。
* 使用 `JSONReader.autoTypeFilter(...)` 做白名單限制。

可以把若依的實作理解為：

> 「**Fastjson2 版的 GenericJackson2JsonRedisSerializer，再加上自己定義的 autoType 白名單策略。**」

---

## 七、如果是自己的專案，怎麼安全地用 Fastjson2 + Redis？

一個合理的模板設計概念（伪碼）如下：

```java
public class SafeFastjsonRedisSerializer implements RedisSerializer<Object> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // 僅允許本專案的 domain / vo 類，杜絕外部雜類
    private static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(
        "com.myapp.domain.",
        "com.myapp.vo."
    );

    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        return JSON.toJSONString(obj, JSONWriter.Feature.WriteClassName)
                   .getBytes(DEFAULT_CHARSET);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        return JSON.parseObject(str, Object.class, AUTO_TYPE_FILTER);
    }
}
```

搭配 Spring 配置：

```java
@Bean
public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    SafeFastjsonRedisSerializer serializer = new SafeFastjsonRedisSerializer();

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    template.afterPropertiesSet();
    return template;
}
```

**關鍵原則：**

1. 使用 **Fastjson2**，避免舊版 Fastjson 1.x 的歷史問題。
2. 開啟 `WriteClassName`（需要多型時）。
3. 一旦要用 autoType，就必須搭配 **嚴格白名單**：

    * 僅限專案自己的 package
    * 不要寫成寬鬆的前綴（例如 `com.`、`org.`、`java.`）

---

## 八、總結：autoType + 白名單 在若依中的定位

1. **autoType 的目的**

    * 讓 Redis 反序列化時，可以根據 JSON 裡的 `@type` 還原成正確的具體類型。
    * 適合需要多型存儲的情境（同一個 key/欄位存放不同類型物件）。

2. **白名單的目的**

    * 關掉「任意類型都能 new」這種危險行為。
    * 只允許專案內「已知、安全的實體類」被還原，降低 RCE 風險。

3. **若依的自訂 Serializer 本質上是：**

    * Fastjson2 + `WriteClassName` + autoType + 白名單
    * 對標的是 Jackson 方案，但選擇了團隊偏好的技術棧與安全策略。

對你在讀若依原始碼來說，這一段的重點不是「它很炫」，而是：

> 只要看到：「`WriteClassName` + `@type` + `autoTypeFilter`」，就要立刻聯想到：
> **多型還原能力 + 反序列化安全控制**，這兩者是綁一起考量的。

```
```
