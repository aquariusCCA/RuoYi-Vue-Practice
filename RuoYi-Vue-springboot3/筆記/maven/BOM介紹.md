開門見山——Maven 裡的 **BOM（Bill of Materials）** 是一種**版本對齊用的 POM**。它不直接引入套件，而是集中宣告一組相互關聯的套件「應該用哪些版本」。專案引用這個 BOM 後，只要寫下依賴的 `groupId`/`artifactId`，**可以省略 `<version>`**，由 BOM 幫你鎖定版本，避免「各自為政」造成版本衝突。

# 你需要掌握的重點

* **本質**：一個以 `packaging=pom` 發佈的 POM，裡面只有 `<dependencyManagement>`（通常不放 `<dependencies>`）。
* **用法（消費者側）**：在自己專案的 `<dependencyManagement>` 中 **以 `type=pom` + `scope=import`** 方式「導入」BOM。
* **效果**：之後在 `<dependencies>` 裡寫相同座標可**省略 `<version>`**；版本由 BOM 統一決定。
* **常見例子**：`spring-boot-dependencies` 就是 Spring 官方提供的 BOM，幫你對齊 Spring 全家桶與常見第三方庫的版本。

## 範例：導入 Spring Boot BOM

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>3.3.4</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- 省略 version，由 BOM 決定 -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
  </dependency>
</dependencies>
```

## 自家團隊想發佈 BOM（供其他專案對齊）

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mycorp</groupId>
  <artifactId>mycorp-bom</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.mycorp</groupId>
        <artifactId>core-utils</artifactId>
        <version>2.4.1</version>
      </dependency>
      <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.2</version>
      </dependency>
      <!-- …集中宣告需對齊的版本… -->
    </dependencies>
  </dependencyManagement>
</project>
```

> 其他專案只要 `import` 這個 BOM，就能沿用這組版本矩陣。

## 與 `dependencyManagement` 的關係

* **BOM 是一種「可被 import 的 dependencyManagement」**。
* 你也可在本專案直接寫 `<dependencyManagement>` 指定版本；**差別**是 BOM 可以**重用/分享**，多專案一致對齊更方便。

## 實務眉角（易踩坑）

1. **多個 BOM 的順序很重要**：同一座標版本若在多個 BOM 都定義，**後引入的會覆蓋先前的**（就近/後者優先的效果）。
2. **BOM 只決定版本，不會自動引入依賴**：真正把 jar 拉進來仍要寫在 `<dependencies>`。
3. **仍可覆蓋**：專案本身的 `<dependencyManagement>` 可覆蓋 BOM 的版本；一般不建議頻繁覆蓋，除非有充分理由。
4. **BOM 與 Plugin 無關**：外掛版本對齊用 `<pluginManagement>`；BOM 只管「依賴庫」版本。
5. **檢查結果**：用

    * `mvn help:effective-pom` 看最終生效版本，
    * `mvn dependency:tree -Dverbose` 追版本來源與衝突調解。

## 什麼時候該用 BOM？

* 你使用某個「生態系」的一組套件（如 Spring、Jakarta、Netty、Cloud Alibaba 等），**需要版本整齊**、少衝突。
* 你們公司/團隊要**制定統一技術基線**，發佈一個自家 BOM，讓所有服務一起跟進。

結論：BOM 是 Maven 生態裡**對齊依賴版本的標準工具**。善用 BOM，可以把「版本管理」從每個專案的局部工作，上升為「平台基線」的統一管理，降低衝突與維護成本。
