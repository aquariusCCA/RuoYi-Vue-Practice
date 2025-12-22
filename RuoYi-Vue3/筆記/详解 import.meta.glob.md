> **參考文章：**
> - [详解 import.meta.glob](https://study.guowei.fun/fe/module/import/import.meta.glob/ "详解 import.meta.glob")

`import.meta.glob` 是在编译时工作的。这意味着当你使用 `import.meta.glob` 时，构建工具（如 Vite）会在构建阶段解析你提供的文件模式，并生成相应的动态导入语句。

具体来说，以下是一些关键点：

# 编译时工作原理

- 文件匹配：
    - 构建工具在编译时会扫描项目目录，查找符合给定模式的文件。例如，如果你使用 `import.meta.glob('./components/*.vue')`，构建工具会找到 `./components` 目录下所有扩展名为 `.vue` 的文件。

- 生成导入语句：
    - 构建工具会生成一个对象，其中每个键是匹配到的文件路径，值是一个动态导入的 Promise。这个对象会在运行时被使用。

- 代码分割：
    - 构建工具还会根据匹配到的文件进行代码分割，确保每个文件被单独打包成一个 chunk，从而实现按需加载。

# 运行时行为
- 动态加载：
    - 在运行时，当你访问 `import.meta.glob` 返回的对象时，每个文件路径对应的 Promise 会被解析，从而异步加载对应的模块。

- 按需加载：
    - 由于返回的是 Promise，你可以根据需要异步加载模块，这有助于优化应用的初始加载时间和性能。

# 示例
假设你有以下项目结构：

```text
/src
  /components
    Button.vue
    Input.vue
    Select.vue
```

你可以在代码中使用 import.meta.glob：

```ts
const modules = import.meta.glob('./components/*.vue');

for (const path in modules) {
  modules[path]().then((module) => {
    console.log(`Loaded module: ${path}`, module);
  });
}
```

# 编译后的代码示例
构建工具（如 Vite）会在编译时将上述代码转换为类似以下的形式：

```ts
const modules = {
  './components/Button.vue': () => import('./components/Button.vue'),
  './components/Input.vue': () => import('./components/Input.vue'),
  './components/Select.vue': () => import('./components/Select.vue')
};

for (const path in modules) {
  modules[path]().then((module) => {
    console.log(`Loaded module: ${path}`, module);
  });
}
```

# 总结
- 编译时：`import.meta.glob` 在编译时解析文件模式并生成动态导入语句。

- 运行时：生成的动态导入语句在运行时按需加载模块。

> 这种机制使得 `import.meta.glob` 成为一种强大的工具，适用于组件库、路由配置和国际化等场景，能够显著提高开发效率和应用性能。