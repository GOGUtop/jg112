# 今日新卡 Local / SillyTavern Local App - Stage 1

这是第一阶段 GitHub 工程：**安卓 APP 内置本地 WebView，启动手机本地 Node，然后打开 `http://127.0.0.1:8000`。**

第一阶段目标很单纯：

- 不接服务器；
- 不做账号登录；
- 不做数据云同步；
- 只验证“APP 能打开本地酒馆”。

> 重要：仓库没有内置 Node Android 二进制，也没有内置 SillyTavern 本体。原因是这两个东西体积大、还涉及不同 Android ABI。你需要按下面步骤把它们放进去。

---

## 目录结构

```text
SillyTavernLocalApp-Stage1/
├─ app/
│  ├─ src/main/java/top/xixisillytavern/local/MainActivity.kt
│  ├─ src/main/assets/
│  │  ├─ mobile-test/              # 没放酒馆时的 Node 测试页
│  │  └─ sillytavern/              # 把完整 SillyTavern 本体放这里
│  └─ src/main/jniLibs/
│     └─ arm64-v8a/
│        └─ libnodebin.so          # 把 Android arm64 Node 可执行文件改名放这里
├─ scripts/
│  ├─ prepare-st.ps1               # Windows 打包 SillyTavern 到 assets
│  └─ prepare-st.sh                # macOS/Linux 打包 SillyTavern 到 assets
└─ docs/
   ├─ NODE_RUNTIME.md
   └─ TROUBLESHOOTING.md
```

---

## 使用步骤

### 1. 导入 Android Studio

直接用 Android Studio 打开整个项目根目录。

建议：

- Android Studio：新版即可；
- JDK：17；
- Android SDK：35；
- 真机优先测试，模拟器也可以但要准备对应 ABI 的 Node。

---

### 2. 放入 Node Android 运行时

大多数手机都是 arm64，所以第一阶段先只放：

```text
app/src/main/jniLibs/arm64-v8a/libnodebin.so
```

注意：

- 文件名必须叫 `libnodebin.so`；
- 内容必须是 **Android/Bionic 平台的 Node 可执行文件**；
- 不是 Windows 的 `node.exe`；
- 不是 Linux x64 的 `node`；
- 也不是普通桌面 Node；
- 如果你用模拟器 x86_64，需要放到 `app/src/main/jniLibs/x86_64/libnodebin.so`。

更多说明看：[`docs/NODE_RUNTIME.md`](docs/NODE_RUNTIME.md)

---

### 3. 放入 SillyTavern 本体

把完整 SillyTavern 目录复制到：

```text
app/src/main/assets/sillytavern/
```

最终至少应该看到：

```text
app/src/main/assets/sillytavern/server.js
app/src/main/assets/sillytavern/package.json
app/src/main/assets/sillytavern/public/
app/src/main/assets/sillytavern/src/
app/src/main/assets/sillytavern/node_modules/
```

Windows 可以用：

```powershell
./scripts/prepare-st.ps1 -SillyTavernPath "D:\SillyTavern" -IncludeData
```

macOS/Linux 可以用：

```bash
bash ./scripts/prepare-st.sh /path/to/SillyTavern --include-data
```

如果不放 SillyTavern，本 APP 会打开一个测试页。测试页只能证明 Node/WebView/127.0.0.1 已跑通，不是真酒馆。

---

### 4. 编译运行

在 Android Studio 里点 Run。

成功流程应该是：

```text
打开 APP
→ 复制 assets 里的 SillyTavern 到 APP 私有目录
→ 启动本地 Node
→ 等待 127.0.0.1:8000 响应
→ WebView 打开本地酒馆
```

---

## 当前阶段已完成

- Android 原生 WebView；
- 本地 `127.0.0.1:8000` 打开；
- 从 APK assets 复制 SillyTavern 到 APP 私有目录；
- 从 `nativeLibraryDir` 启动内置 Node；
- 自动生成最基础 `config.yaml`；
- 没有放入酒馆时，自动打开 Node 测试页；
- 日志写入 `adb logcat`，tag 是 `ST_LOCAL_STAGE1`。

---

## 下一阶段建议

第二阶段再做：

1. 首次启动选择账号；
2. 从服务器下载账号 `data.zip`；
3. 写入 `filesDir/sillytavern/data/账号名`；
4. 文件变化监听；
5. 退出/切后台上传 data；
6. 账号锁；
7. 管理后台。



## 2026-07-06 修复

修复 GitHub Actions / Gradle 编译失败：`compileDebugJavaWithJavac (1.8)` 与 `compileDebugKotlin (17)` 目标版本不一致。现在 Java 和 Kotlin 均统一为 JVM 17。

同时移除了 AndroidManifest.xml 里的 `android:extractNativeLibs`，改由 `app/build.gradle` 的 `packaging.jniLibs.useLegacyPackaging = true` 管理，避免 AGP 新版本警告。
