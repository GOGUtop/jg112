# 常见问题

## 1. 打开 APP 显示“没找到 Node 运行时”

说明没有放：

```text
app/src/main/jniLibs/arm64-v8a/libnodebin.so
```

或者你的手机不是 arm64，需要放对应 ABI。

---

## 2. 显示 Node 已启动，但 127.0.0.1:8000 没响应

看日志：

```bash
adb logcat | grep ST_LOCAL_STAGE1
```

常见原因：

- Node 二进制不是 Android 版本；
- `server.js` 不存在；
- `node_modules` 没有打包；
- SillyTavern 版本和 Node 版本不兼容；
- 8000 端口被占用。

---

## 3. 只打开了“Node 本地启动成功”页面

这不是酒馆，是测试页。

说明你还没有把真正的 SillyTavern 放进：

```text
app/src/main/assets/sillytavern/
```

最终必须存在：

```text
app/src/main/assets/sillytavern/server.js
```

---

## 4. 修改了 assets 里的 SillyTavern，但手机还是旧的

卸载 APP 后重新安装，或者清除 APP 数据。

第一阶段为了简单，只做了基础覆盖逻辑，后面第二阶段再做版本号/manifest 精确更新。

---

## 5. 真机能跑，模拟器不能跑

真机一般是 arm64-v8a，模拟器多半是 x86_64。你需要准备对应 ABI 的 Node：

```text
app/src/main/jniLibs/x86_64/libnodebin.so
```

