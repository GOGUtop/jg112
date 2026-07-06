# Node Android 运行时说明

这个工程第一阶段采用“把 Android Node 可执行文件当作 native library 打包”的方式：

```text
app/src/main/jniLibs/arm64-v8a/libnodebin.so
```

虽然扩展名叫 `.so`，但它的内容应该是 **Node 可执行文件**。这样做是为了让 Android 在安装 APK 时把文件释放到 `nativeLibraryDir`，APP 再从那里执行它。

## 为什么不直接放 assets/runtime/node？

Android 10 之后，从 APP 私有可写目录执行下载/复制出来的二进制会遇到更严格限制。把可执行文件随 APK 作为 native library 打包，是更稳的路线。

## 你需要准备什么？

需要准备适配 Android 的 Node 可执行文件：

- arm64 手机：`arm64-v8a/libnodebin.so`
- x86_64 模拟器：`x86_64/libnodebin.so`

重点：它必须是 Android/Bionic 目标构建，普通 Linux/Windows/macOS Node 不能用。

## 文件名要求

必须叫：

```text
libnodebin.so
```

不是：

```text
node
node.exe
libnode.so
```

## 放置路径

常用真机：

```text
app/src/main/jniLibs/arm64-v8a/libnodebin.so
```

模拟器：

```text
app/src/main/jniLibs/x86_64/libnodebin.so
```

## 如何判断是不是 Node 没跑起来？

连接手机后执行：

```bash
adb logcat | grep ST_LOCAL_STAGE1
```

如果看到类似：

```text
No such file or directory
Permission denied
Exec format error
CANNOT LINK EXECUTABLE
```

基本就是 Node 二进制不对，或者缺 Android 依赖库。

