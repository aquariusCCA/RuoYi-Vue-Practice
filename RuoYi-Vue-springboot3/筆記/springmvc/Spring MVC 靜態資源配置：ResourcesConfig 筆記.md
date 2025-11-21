# Spring MVC 靜態資源配置：`ResourcesConfig` 筆記

## 1. 這段程式碼到底負責什麼？

`ResourcesConfig` 是用來**擴充 Spring MVC** 的靜態資源處理能力。
它負責建立 **URL → 實際檔案路徑** 的映射規則，避免每次都用 Controller 自己讀檔案。

---

## 2. 本地上傳檔案路徑映射

```java
registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
        .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");
```

### 目的

讓使用者上傳後存到**本地磁碟**的檔案，可以直接透過 URL 訪問，而不用自己寫 API 回傳檔案。

### 行為解讀

* 假設 `RESOURCE_PREFIX = "/profile"`
* 假設 `RuoYiConfig.getProfile()` = `/home/ruoyi/uploadPath`

則映射為：

| URL 路徑        | 實際對應磁碟路徑                    |
| ------------- | --------------------------- |
| `/profile/**` | `/home/ruoyi/uploadPath/**` |

### 使用範例

如果你存了一張頭像到：

```
/home/ruoyi/uploadPath/avatar/user1.png
```

則前端可直接用：

```
http://localhost:8080/profile/avatar/user1.png
```

Spring MVC 會自動回傳該檔案，不會走任何 Controller。

### 為何需要？

因為上傳檔案不可能放進 jar 的 classpath，因此需要手動建立 URL → 檔案系統的對應。

---

## 3. Swagger UI 靜態資源配置

```java
registry.addResourceHandler("/swagger-ui/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
        .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
```

### 目的

讓 Swagger UI（前端 SPA）正常載入其 HTML / JS / CSS。

Swagger UI 的檔案都在 webjar 裡，路徑為：

```
classpath:/META-INF/resources/webjars/springfox-swagger-ui/
```

### 映射行為

| URL 路徑           | 對應位置               |
| ---------------- | ------------------ |
| `/swagger-ui/**` | webjar (classpath) |

若訪問：

```
/swagger-ui/index.html
```

Spring 會從 webjar 中讀取 `index.html`。

### Cache 設定

```
maxAge = 5 小時
cachePublic = 可被代理、CDN 快取
```

目的是降低 Swagger 靜態檔大量重複載入，提升效能。

---

## 4. 為什麼若依需要這份配置？

### (1) 上傳檔案功能

金融、政府後台系統必須允許上傳：

* 頭像
* 附件
* PDF 合約
* 圖片

這些都儲存於磁碟，必須透過 URL 提供存取，因此 `/profile/**` → 本地磁碟 是必要的。

### (2) Swagger 界面可正常展示

Swagger UI 本質上是一個前端 SPA，只是被打包成 webjar。

如果不映射 `/swagger-ui/**`，Swagger UI 將無法載入 JS/CSS。

---

## 5. 總結（快速記憶）

| 功能         | URL              | 找檔案位置                    | 用途                   |
| ---------- | ---------------- | ------------------------ | -------------------- |
| 本地上傳檔案訪問   | `/profile/**`    | `file:{uploadPath}/**`   | 讀取磁碟上的使用者上傳檔案        |
| Swagger UI | `/swagger-ui/**` | `classpath:/webjars/...` | 讓 Swagger UI 正常運作並快取 |

---