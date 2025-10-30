根據如下筆記內容，幫我命名一個文件名稱

# Maven 與 Spring Boot 版本管理：實戰備忘錄

> 給自己看的重點：**BOM 只管相依（dependencies），不管外掛（plugins）**。
> 想讓 `spring-boot-maven-plugin` 自動對齊版本，要嘛**用 `starter-parent` 當父 POM**，要嘛**自己在父 POM 的 `pluginManagement` 鎖外掛版本**。

---

## 1. 名詞釐清

### 1.1 BOM（Bill of Materials）

* 典型代表：`spring-boot-dependencies`（`type=pom`, `scope=import`）。
* 作用：在 `<dependencyManagement>` 中**只**提供**相依套件**版本矩陣。
* 限制：**不會**影響 `<build><pluginManagement>`，也**不會**幫你決定外掛版本。

> NOTE: My-RuoYi/筆記/maven/BOM介紹.md

### 1.2 Parent POM（父 POM）

* 典型代表：`spring-boot-starter-parent`。
* 作用：子模組會**繼承**父 POM 的：

    * `<dependencyManagement>`（含相依版本）
    * `<build><pluginManagement>`（含外掛版本）
    * 以及其他預設（資源過濾、編譯/測試參數等）

### 1.3 `dependencyManagement` vs `pluginManagement`

* `dependencyManagement`：管理**相依套件**的版本。
* `pluginManagement`：管理**Maven 外掛**（如 `spring-boot-maven-plugin`、`maven-compiler-plugin`）的版本與預設配置。

---

## 2. 為什麼在引入 `spring-boot-dependencies`（BOM）後仍要寫 `spring-boot-maven-plugin` 版本？

* 因為你是用 **BOM 匯入（import）**，Maven 只會帶入**相依套件**版本，不會帶入 plugin 版本。
* 只有把含 `pluginManagement` 的 POM 放在 `<parent>` 時，子模組才會繼承外掛版本。

> 關鍵差異：
>
> * `<parent>`：繼承 **dependencies + plugins**
> * `<dependencyManagement>`（import BOM）：**只有 dependencies**

---

## 3. 正確做法（避免版本漂移）

### 方案 A｜最省心：用官方父 POM

**適用**：沒有公司自製父 POM；想少管設定
**作法**：

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.4</version>
</parent>

<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <!-- 不用寫 <version>，由 parent 的 pluginManagement 管 -->
    </plugin>
  </plugins>
</build>
```

**優點**：`spring-boot-maven-plugin` 等常見外掛自動對齊。
**缺點**：一個專案只能有一個 `<parent>`；若公司已有父 POM，就衝突。

---

### 方案 B｜企業常態：自製父 POM + 匯入 BOM + 自管 `pluginManagement`

**適用**：多模組、公司標準化建置
**作法**（父 POM）：

```xml
<properties>
  <java.version>17</java.version>
  <spring-boot.version>3.5.4</spring-boot.version>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<build>
  <pluginManagement>
    <plugins>
      <!-- 將 Boot 外掛與常用外掛統一定版 -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
          <parameters>true</parameters>
          <source>${java.version}</source>
          <target>${java.version}</target>
          <encoding>${project.build.sourceEncoding}</encoding>
        </configuration>
      </plugin>
      <!-- 依需要加入 surefire, jar, resources, failsafe 等外掛版本 -->
    </plugins>
  </pluginManagement>
</build>
```

**子模組**只需：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <!-- 不用再寫 <version> -->
    </plugin>
  </plugins>
</build>
```

**優點**：公司可控、統一版本、可重現建置。
**關鍵**：**用同一個屬性（`spring-boot.version`）同時約束 BOM 與 plugin 版本**。

---

## 4. 推薦父 POM骨架（多模組實務）

**父 POM（聚合 + 管理）**

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.my</groupId>
  <artifactId>my-parent</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>common</module>
    <module>service-a</module>
    <module>service-b</module>
  </modules>

  <properties>
    <java.version>17</java.version>
    <spring-boot.version>3.5.4</spring-boot.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring-boot.version}</version>
        </plugin>

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <parameters>true</parameters>
            <source>${java.version}</source>
            <target>${java.version}</target>
            <encoding>${project.build.sourceEncoding}</encoding>
          </configuration>
        </plugin>

        <!-- 其他外掛統一定版：surefire, failsafe, jar, resources, shade 等 -->
      </plugins>
    </pluginManagement>
  </build>
</project>
```

**子模組**

```xml
<project>
  <parent>
    <groupId>com.my</groupId>
    <artifactId>my-parent</artifactId>
    <version>1.0.0</version>
  </parent>

  <artifactId>service-a</artifactId>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 其他依賴直接不寫版本，由 BOM 管理 -->
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <!-- 不寫版本，走父 POM 的 pluginManagement -->
      </plugin>
    </plugins>
  </build>
</project>
```

---