> 參考文章：[springMVC之@InitBinder的用法](https://blog.csdn.net/weixin_43888891/article/details/127348918 "springMVC之@InitBinder的用法")

@InitBinder 是 Spring MVC 在「參數綁定階段」動手腳的工具，用來**客製化 Controller 的資料綁定與格式轉換**，典型用途有：

* 自訂字串前後空白處理、大小寫處理
* 統一日期格式轉換（字串 → Date / LocalDate）
* 對某些欄位做防護（例如 XSS 過濾）
* 指定只針對某些欄位、某些方法生效

> 关于 Date 属性绑定器有两种方案：使用 spring 提供的 CustomDateEditor，另外一种就是自定义 PropertyEditorSuppotr。

---

## 一、基本概念：@InitBinder 什麼時候被呼叫？

當你在 Controller 裡寫：

```java
@PostMapping("/createUser")
public String createUser(UserForm form) { ... }
```

Spring 會用 `WebDataBinder` 把 HTTP 參數（query string、form-data 等）綁到 `UserForm` 上。

如果這個 Controller 裡有：

```java
@InitBinder
public void initBinder(WebDataBinder binder) {
    // 在這裡客製化 binder
}
```

那麼在 Spring 開始對 `UserForm` 做屬性賦值前，會先呼叫 `initBinder(...)`，你就可以在這裡註冊：

* `PropertyEditor`（舊的 API）
* `Formatter` / `Converter`（新式的型別轉換）
* 綁定白名單/黑名單欄位等

---

## 二、最簡單範例：把所有字串 trim，避免「空白也算有值」

假設你的表單物件：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {
    private String username;
    private String email;
    // getter / setter 省略
}
```

Controller：

```java
@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        System.out.println("1111");
        // StringTrimmerEditor(true) 表示：
        // trim 後如果是空字串，轉成 null
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String publicTest(UserForm userForm) {
        // 這裡拿到的 form.username / form.email 已經是 trim 過的
        // "  " -> null, " abc " -> "abc"
        System.out.println("UserForm: " + userForm);
        return "OK";
    }
}
```

前端用 application/x-www-form-urlencoded 或 HTML form 送資料：

* 這樣會經過 WebDataBinder
* 你的 @InitBinder + StringTrimmerEditor(true) 就會生效
* 收到的 username / password 就會是去掉首尾空白（甚至可以變 null）

重點：

* `@InitBinder` 方法**不需要回傳值**，Spring 會自動使用你對 `binder` 做的設定。
* `registerCustomEditor(String.class, editor)` = 對所有 `String` 屬性生效。

---

## 三、常見用途：處理日期格式（字串 → Date / LocalDate）

假設你的表單：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderForm {
    private Date orderDate;
    // getter / setter ...
}
```

前端送上來的字串是 `2025-11-17`，你希望自動轉成 `Date`。

```java
@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        System.out.println("1111");
        // 指定日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false); // 嚴格解析，不接受 2025-13-40 這種鬼東西

        // S
        // CustomDateEditor(格式, 允不允許 null 值)
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String publicTest(OrderForm orderForm) {
        System.out.println("OrderForm: " + orderForm);
        return "OK";
    }
}
```

---

## 四、只綁定「指定欄位」：避免被多傳欄位污染

有時你不希望前端自由傳一堆欄位過來就都被綁定，例如防止「多傳欄位覆蓋你不想被改的屬性」。
`WebDataBinder` 提供：

* `setAllowedFields(...)`：只允許綁這些欄位（白名單）
* `setDisallowedFields(...)`：禁止綁這些欄位（黑名單）

範例：只允許綁 `username` 和 `email`：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {
    private String username;
    private String role;
    private String email;
}
```

```java
@Controller
public class TestController {
    @InitBinder("userForm")
    public void initBinder(WebDataBinder binder) {
        System.out.println("1111");
        binder.setAllowedFields("username", "email");
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String publicTest(@ModelAttribute("userForm")UserForm userForm) {
        // 只有 username, email 會被綁定
        // 前端即使多傳例如 "role=ADMIN"，也不會被綁到物件上
        System.out.println("UserForm: " + userForm);
        return "OK";
    }
}
```

注意這裡的重點：

### 1. @InitBinder("userForm")

* 括號內的 `"userForm"` 是 **model attribute name**

* 預設情況下，如果你寫：

  ```java
  public String updateUser(@ModelAttribute("userForm") UserForm form) { ... }
  ```

  Spring 就會建立一個名為 `userForm` 的 model attribute。

* `@InitBinder("userForm")` 代表：**只在綁定這個名稱的物件時套用這個 binder 設定**。

如果你省略括號：

```java
@InitBinder
public void initBinder(WebDataBinder binder) { ... }
```

那就會對這個 Controller 內所有參數綁定生效。

---

## 五、多個 @InitBinder 方法時的行為

一個 Controller 可以有多個 `@InitBinder` 方法，例如：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {
    private String username;
    private String role;
    private String email;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginForm {
    private String username;
    private String password;
}
```

```java
@Controller
public class TestController {
    @InitBinder
    public void globalBinder(WebDataBinder binder) {
        // 全部綁定都 trim 字串
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @InitBinder("userForm")
    public void userFormBinder(WebDataBinder binder) {
        // 只有 userForm 物件才限制欄位
        binder.setAllowedFields("username", "email");
    }

    @PostMapping("/api/public/test/update")
    @ResponseBody
    public String updateUser(@ModelAttribute("userForm") UserForm userForm) {
        System.out.println("UserForm: " + userForm);
        return "OK";
    }

    @PostMapping("/api/public/test/login")
    @ResponseBody
    public String login(@ModelAttribute("LoginForm") LoginForm loginForm) {
        System.out.println("LoginForm: " + loginForm);
        return "OK";
    }
}
```

* `globalBinder(...)` 會對 `userForm` 和 `loginForm` 都生效（字串 trim）。
* `userFormBinder(...)` 只會對 `userForm` 生效（欄位白名單）。

---

## 六、使用 PropertyEditorSupport 客製綁定邏輯

`StringTrimmerEditor`、`CustomDateEditor` 本身就是 Spring 幫你實作好的 `PropertyEditorSupport`。
如果現成的 Editor 不夠用，你可以自己寫一個 class 去繼承 `PropertyEditorSupport`。

### 1. 基本用法範例：只處理某個欄位的字串（trim + 轉大寫）

先定義一個 Editor：

```java
public class UsernameEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? value.toString() : "");
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null) {
            setValue(null);
            return;
        }
        // 自訂處理邏輯：trim + 轉大寫
        String value = text.trim().toUpperCase();
        // 最後一定要 setValue(...)，Spring 才能把它塞回目標物件的 property
        setValue(value);
    }
}

```

在 Controller 裡的 `@InitBinder` 透過 `registerCustomEditor` 註冊：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {
    private String username;
}
```

```java
@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 第一種：只針對特定欄位使用這個 Editor
        // 這裡的 "username" 是目標物件上的屬性名稱
        binder.registerCustomEditor(String.class, "username", new UsernameEditor());

        // 如果你還要全域 trim，可以再加一個 Editor（注意順序與覆蓋關係）
        // binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String updateUser(UserForm userForm) {
        // form.getUsername() 會是 trim + toUpperCase() 過後的結果
        // form.getEmail() 則不會受到 UsernameEditor 影響
        System.out.println("UserForm: " + userForm);
        return "OK";
    }
}
```

重點：

* `registerCustomEditor(目標型別, 屬性名稱, editor)`：
  只對**指定屬性**生效（這是常見用法，避免影響所有 `String`）。
* 如果用 `registerCustomEditor(目標型別, editor)`（沒填屬性名），就會對該型別所有屬性生效。

### 2. 範例：把 `"Y"/"N"` 字串轉成 Boolean

表單常見：用 `<select>` 或 `<input>` 傳 `"Y"` / `"N"`，希望自動綁成 `Boolean`。

#### (1) Editor 實作

```java
public class YesNoBooleanEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        Boolean value = (Boolean) getValue();
        return (value != null && value) ? "Y" : "N";
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if(text == null) {
            setValue(null);
            return;
        }

        String value = text.trim().toUpperCase();
        switch (value) {
            case "Y":
            case "YES":
            case "1":
                setValue(Boolean.TRUE);
                break;
            case "N":
            case "NO":
            case "0":
                setValue(Boolean.FALSE);
                break;
            default:
                throw new IllegalArgumentException("Invalid boolean value: " + text);
        }

    }
}
```

#### (2) 在 @InitBinder 註冊

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerForm {
    private Boolean vip;
}
```

```java
@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 對所有 Boolean 欄位都套用 Yes/No 規則（如果專案規則一致）
        // binder.registerCustomEditor(Boolean.class, new YesNoBooleanEditor());

        // 或者只對某一欄位生效
        binder.registerCustomEditor(Boolean.class, "vip", new YesNoBooleanEditor());
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String updateUser(CustomerForm customerForm) {
        // form.getVip() 會自動把 "Y"/"N" 轉成 true / false
        System.out.println("CustomerForm: " + customerForm);
        return "OK";
    }
}
```

### 3. 範例：把「逗號分隔字串」轉成 List<Long>

例如前端傳 `"1,2,3"` 希望綁成 `List<Long>`（雖然實務上更建議用 Converter，但你問的是 `PropertyEditorSupport`，就用它示範）。

#### (1) Editor 實作

```java
public class LongListEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        List<Long> value = (List<Long>) getValue();
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.isEmpty()) {
            setValue(Collections.emptyList());
            return;
        }
        String[] parts = text.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Long.parseLong(trimmed));
            }
        }
        setValue(ids);
    }
}
```

#### (2) 在 @InitBinder 綁某個欄位

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleForm {
    private List<Long> roleIds;
    // getter / setter ...
}

@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 注意：目標型別寫 List.class，屬性名稱指定 "roleIds"
        binder.registerCustomEditor(List.class, "roleIds", new LongListEditor());
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String updateUser(RoleForm roleForm) {
        // 前端傳 roleIds=1,2,3 會自動變成 List<Long> [1,2,3]
        System.out.println("RoleForm: " + roleForm);
        return "OK";
    }
}
```

---