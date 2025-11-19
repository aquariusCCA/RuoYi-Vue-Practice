# **FileTypeUtils 做什麼？**

這支工具類提供三種方式來判斷「檔案的副檔名」：

1. **透過 File 物件取得副檔名**
2. **透過字串檔名取得副檔名**
3. **透過檔案的二進位 byte[] 判斷檔案格式（依照檔案頭 Magic Number）**

它沒有做 MIME 類型判斷、也沒有完整檢查全部格式，只做最基本的操作。

---

# 1. `getFileType(File file)`

### **功能**

* 如果 `file == null` → 回傳空字串
* 否則取得檔名，再委派給 `getFileType(String fileName)`

### **潛在問題**

* 只是簡單的 `.suffix` 判斷，完全不檢查內容
* 不能處理 `aaa.tar.gz` 這種多重副檔名，只會回傳 `gz`

### **測試範例**

```java
@Test
public void testGetFileTypeWithFile() {
    File file1 = new File("example.txt");
    File file2 = new File("photo.jpeg");
    File file3 = new File("no_extension");
    File file4 = null;

    System.out.println(FileTypeUtils.getFileType(file1)); // txt
    System.out.println(FileTypeUtils.getFileType(file2)); // jpeg
    System.out.println(FileTypeUtils.getFileType(file3)); // ""
    System.out.println(FileTypeUtils.getFileType(file4)); // ""
}
```

---

# 2. `getFileType(String fileName)`

### **功能**

* 找到最後一個 `.`
* 沒有 `.` → 回傳空字串
* 回傳小寫副檔名，例如 `"TXT"` → `"txt"`

### **測試範例**

```java
@Test
public void testGetFileTypeWithString() {
    System.out.println(FileTypeUtils.getFileType("ruoyi.txt"));   // txt
    System.out.println(FileTypeUtils.getFileType("report.PDF"));  // pdf
    System.out.println(FileTypeUtils.getFileType("archive.tar.gz")); // gz
    System.out.println(FileTypeUtils.getFileType("noext"));       // ""
}
```

---

# 3. `getFileExtendName(byte[] photoByte)`

### **功能**

透過「檔案頭 bytes」判斷格式（Magic Number detection）。
檢查順序與邏輯如下：

| 格式  | Magic Number                                         | 程式碼檢查方式         |                 |
| --- | ---------------------------------------------------- | --------------- | --------------- |
| GIF | `47 49 46 38 37                                      | 39 61`          | photoByte[0..5] |
| JPG | `FF D8 FF`，但 RuoYi 用錯誤方式：檢查 photoByte[6~9] = J F I F | 偏弱、不可靠          |                 |
| BMP | `42 4D`                                              | photoByte[0..1] |                 |
| PNG | `89 50 4E 47`，但 RuoYi 只檢查第 1,2,3 byte                | 不完整             |                 |

> 文件头信息是存储在文件开头的一些特殊信息，通常用于表示文件的类型和格式。每种文件类型的头信息都是不同的。例如，JPEG图像文件的头信息通常为“FF D8”，PDF文件的头信息通常为“25 50 44 46”。

> **參考文章：**
>
> [Java依据文件头获取文件类型](https://www.cnblogs.com/leigq/p/13406540.html "Java依据文件头获取文件类型")
> [java如何判断一个文件的格式](https://docs.pingcode.com/baike/303807 "java如何判断一个文件的格式")
> [JAVA判断文件类型，通过文件头信息判断是什么文件类型](https://blog.csdn.net/weixin_44723016/article/details/126507546 "JAVA判断文件类型，通过文件头信息判断是什么文件类型")


### **批判點（需要懷疑）**

* JPG 判斷錯誤，它檢查的是 **JFIF ASCII 字串** (`J F I F`) 而不是標準 JPG Magic Number (`FF D8 FF`)。
* PNG Magic Number 是 `89 50 4E 47`，但這裡的邏輯居然從 index=1 開始判斷，更不可靠。

但此工具類原始碼就是 RuoYi 內建的，你需要知道它的實際行為，而不是正確標準。

### **測試範例（模擬 Magic Number）**

```java
@Test
public void testGetFileExtendName() {

    // 模擬 GIF：GIF87a
    byte[] gif = new byte[] {71, 73, 70, 56, 55, 97};
    System.out.println(FileTypeUtils.getFileExtendName(gif)); // GIF

    // 模擬 BMP：BM
    byte[] bmp = new byte[] {66, 77, 0, 0};
    System.out.println(FileTypeUtils.getFileExtendName(bmp)); // BMP

    // 模擬 PNG（雖然判斷不標準，但符合其條件）
    byte[] png = new byte[] {0, 80, 78, 71};
    System.out.println(FileTypeUtils.getFileExtendName(png)); // PNG

    // 模擬 JPG（符合 RuoYi 錯誤的 JFIF 判斷）
    byte[] jpg = new byte[] {0,0,0,0,0,0,74,70,73,70};
    System.out.println(FileTypeUtils.getFileExtendName(jpg)); // JPG

    // 隨機 byte，預設會回傳 JPG
    byte[] unknown = new byte[] {1,2,3,4,5,6,7,8,9};
    System.out.println(FileTypeUtils.getFileExtendName(unknown)); // JPG
}
```

---

# **結論（開門見山）**

1. `getFileType(File)`、`getFileType(String)`
   → 單純取得副檔名，完全不做內容檢查

2. `getFileExtendName(byte[])`
   → 用很弱的 Magic Number 判斷
   → GIF / BMP 基本能判斷
   → PNG / JPG 判斷不標準，可能誤判
   → 預設回傳 `"JPG"`（即便不是 JPG）

---