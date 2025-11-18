# 0. org.springframework.beans.BeanUtils — 背景補充

Ruoyi 的 `BeanUtils` 是：

```java
public class BeanUtils extends org.springframework.beans.BeanUtils
```

也就是 **繼承 Spring 官方的 BeanUtils**。
Spring 的 BeanUtils 內建許多 Bean 操作方法，最常用的是：

### ✔ `copyProperties(Object source, Object target)`

* 將來源物件 (`source`) 的屬性複製到目標物件 (`target`)
* 屬性名稱需相同
* setter/getter 必須存在
* 不會複製 `static`、`final` 屬性
* 為 **淺拷貝**
* 若 `source` 有欄位但 `target` 沒有 setter，則不會複製

### ✔ `instantiateClass(Class<T> clazz)`

反射建立物件，相當於 `clazz.newInstance()` 的安全版本。

---

# 1. Ruoyi BeanUtils 工具類功能總覽

Ruoyi 的 `BeanUtils` 在 Spring 的基礎上加入：

| 方法                 | 用途                        |
| ------------------ | ------------------------- |
| copyBeanProp       | 封裝 Spring copyProperties  |
| getSetterMethods   | 列出所有 setter 方法            |
| getGetterMethods   | 列出所有 getter 方法            |
| isMethodPropEquals | 判斷 getter/setter 是否同屬同一屬性 |

---

# 2. 重要常數說明

```java
private static final int BEAN_METHOD_PROP_INDEX = 3;
```

所有 getter/setter 名稱長這樣：

* `getName`
* `setName`

屬性名稱起始位置 = 3（跳過 "get"/"set"）

例：

```java
"getName".substring(3) → "Name"
```

---

# 3. Getter / Setter 正則解析

```java
private static final Pattern GET_PATTERN = Pattern.compile("get(\\p{javaUpperCase}\\w*)");
private static final Pattern SET_PATTERN = Pattern.compile("set(\\p{javaUpperCase}\\w*)");
```

### 規則要求：

| 要求             | 說明          |
| -------------- | ----------- |
| 必須是 get/set 開頭 | 不能是 is()    |
| 4th 字元是大寫      | getname 不符合 |
| 之後是 \w*        | 字母/數字/底線    |

### 不支援：

* `isActive()`（boolean getter）
* `getname()`（首字母不大寫）

---

# 4. copyBeanProp — 屬性複製方法

```java
public static void copyBeanProp(Object dest, Object src)
{
    try {
        copyProperties(src, dest);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### ✔ 功能

* 包裝 Spring `copyProperties`
* 將 `src` 的相同屬性值複製到 `dest`

### ✔ 特性

* **淺拷貝**
* 不會複製 static 屬性
* 需要 setter/getter
* 出錯只印 stacktrace

---

## ✔ 測試程式 — copyBeanProp

```java
public class Person {
    private String name;
    private Integer age;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}

public class BeanUtilsCopyTest {
    public static void main(String[] args) {
        Person src = new Person();
        src.setName("Alice");
        src.setAge(20);

        Person dest = new Person();
        dest.setName("Bob");
        dest.setAge(30);

        System.out.println("Before copy:");
        System.out.println("src  = " + src);
        System.out.println("dest = " + dest);

        BeanUtils.copyBeanProp(dest, src);

        System.out.println("After copy:");
        System.out.println("src  = " + src);
        System.out.println("dest = " + dest);
    }
}
```

---

# 5. getSetterMethods — 取得所有 setter

```java
public static List<Method> getSetterMethods(Object obj)
```

### ✔ 動作流程

1. 取得所有 public 方法
2. 名稱符合 `setXxx`
3. 參數數量 = 1
4. 加入清單

### ✔ 限制

* 不抓 private setter
* 不抓多參數 setter

---

## ✔ 測試程式 — getSetterMethods

```java
public class BeanUtilsSetterTest {
    public static void main(String[] args) {
        Person p = new Person();
        List<Method> setters = BeanUtils.getSetterMethods(p);

        System.out.println("Setter methods:");
        for (Method m : setters) {
            System.out.println(" - " + m.getName() 
                + " | paramType = " + m.getParameterTypes()[0].getSimpleName());
        }
    }
}
```

---

# 6. getGetterMethods — 取得所有 getter

```java
public static List<Method> getGetterMethods(Object obj)
```

### ✔ 動作流程

1. 取得所有 public 方法
2. 名稱符合 `getXxx`
3. 無參數
4. 加入清單

### ✔ 限制

* 不抓 `isXxx()`（boolean getter）
* 需要 public getter

---

## ✔ 測試程式 — getGetterMethods

```java
public class BeanUtilsGetterTest {
    public static void main(String[] args) {
        Person p = new Person();
        List<Method> getters = BeanUtils.getGetterMethods(p);

        System.out.println("Getter methods:");
        for (Method m : getters) {
            System.out.println(" - " + m.getName() 
                + " | returnType = " + m.getReturnType().getSimpleName());
        }
    }
}
```

---

# 7. isMethodPropEquals — 判斷是否同一屬性

```java
public static boolean isMethodPropEquals(String m1, String m2)
{
    return m1.substring(3).equals(m2.substring(3));
}
```

### ✔ 功能

判斷如下是否同屬性：

```java
getName  → Name
setName  → Name
```

結果：true

---

## ✔ 測試程式 — isMethodPropEquals（字串版）

```java
public class BeanUtilsMethodPropTest {
    public static void main(String[] args) {
        System.out.println(BeanUtils.isMethodPropEquals("getName", "setName")); // true
        System.out.println(BeanUtils.isMethodPropEquals("getName", "setAge"));  // false
        System.out.println(BeanUtils.isMethodPropEquals("getAge",  "setAge"));  // true
    }
}
```

---

## ✔ 搭配反射 Method 版本

```java
public class BeanUtilsMethodPropTest2 {
    public static void main(String[] args) throws Exception {
        Method getName = Person.class.getMethod("getName");
        Method setName = Person.class.getMethod("setName", String.class);
        Method setAge  = Person.class.getMethod("setAge", Integer.class);

        System.out.println(
            BeanUtils.isMethodPropEquals(getName.getName(), setName.getName())
        ); // true

        System.out.println(
            BeanUtils.isMethodPropEquals(getName.getName(), setAge.getName())
        ); // false
    }
}
```

---

# 8. 實務應用場景

| 場景                  | 使用方法                    |
| ------------------- | ----------------------- |
| DTO ↔ Entity 自動欄位同步 | copyBeanProp            |
| Excel 匯出            | getGetterMethods        |
| Excel 匯入            | getSetterMethods        |
| Bean → Map          | getGetterMethods        |
| Map → Bean          | getSetterMethods        |
| 自動欄位映射（像 MyBatis）   | getter + setter pairing |
| 大型系統資料轉換            | copyBeanProp            |

---