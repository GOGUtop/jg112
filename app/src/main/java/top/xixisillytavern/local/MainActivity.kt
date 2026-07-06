package top.xixisillytavern.local

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var statusView: TextView

    private var nodeProcess: Process? = null
    private val port = 8000
    private val localUrl = "http://127.0.0.1:$port/"

    companion object {
        private const val TAG = "ST_LOCAL_STAGE1"
        private var started = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        configureWebView()
        showStatus("正在准备本地酒馆……")

        thread(name = "st-bootstrap") {
            bootstrapAndStart()
        }
    }

    private fun buildUi() {
        root = FrameLayout(this)
        webView = WebView(this)
        statusView = TextView(this)

        statusView.setTextColor(Color.rgb(38, 38, 48))
        statusView.textSize = 15f
        statusView.gravity = Gravity.CENTER
        statusView.setPadding(dp(22), dp(22), dp(22), dp(22))
        statusView.setBackgroundColor(Color.rgb(247, 251, 255))

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(statusView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    private fun bootstrapAndStart() {
        try {
            val nodeBin = File(applicationInfo.nativeLibraryDir, "libnodebin.so")
            if (!nodeBin.exists()) {
                fail("""
                    没找到 Node 运行时。

                    请把 Android arm64 的 Node 可执行文件放到：
                    app/src/main/jniLibs/arm64-v8a/libnodebin.so

                    注意：文件名必须叫 libnodebin.so，实际内容是可执行 Node，不是普通 Linux/Windows 版 node。
                """.trimIndent())
                return
            }

            val usingRealSt = assetPathExists("sillytavern/server.js")
            val assetRoot = if (usingRealSt) "sillytavern" else "mobile-test"
            val workDir = File(filesDir, "sillytavern")
            val marker = File(workDir, ".bundle-source")
            val expectedMarker = if (usingRealSt) "sillytavern" else "mobile-test"

            if (!workDir.exists() || !File(workDir, "server.js").exists() || marker.readTextOrNull() != expectedMarker) {
                showStatus("正在复制 ${if (usingRealSt) "SillyTavern" else "Node 测试页"} 到 APP 本地目录……")
                workDir.deleteRecursively()
                workDir.mkdirs()
                copyAssetDir(assetRoot, workDir)
                marker.writeText(expectedMarker)
            }

            if (usingRealSt) {
                ensureSillyTavernConfig(workDir)
            }

            if (!started) {
                started = true
                startNode(nodeBin, workDir)
            } else {
                showStatus("本地 Node 已启动，正在打开页面……")
            }

            if (waitUntilReady()) {
                runOnUiThread {
                    statusView.visibility = View.GONE
                    webView.loadUrl(localUrl)
                }
            } else {
                fail("""
                    Node 已尝试启动，但 $localUrl 没有响应。

                    请用 adb logcat 搜索：$TAG
                    常见原因：
                    1. Node 二进制不是 Android 可执行文件；
                    2. SillyTavern 文件没有放完整；
                    3. node_modules 没有随本体一起打包；
                    4. 端口 8000 被占用。
                """.trimIndent())
            }
        } catch (t: Throwable) {
            Log.e(TAG, "bootstrap failed", t)
            fail("启动失败：${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun startNode(nodeBin: File, workDir: File) {
        showStatus("正在启动本地 Node：127.0.0.1:$port ……")
        val pb = ProcessBuilder(nodeBin.absolutePath, "server.js")
            .directory(workDir)
            .redirectErrorStream(true)

        pb.environment()["HOME"] = filesDir.absolutePath
        pb.environment()["TMPDIR"] = cacheDir.absolutePath
        pb.environment()["NO_UPDATE_NOTIFIER"] = "1"
        pb.environment()["STAGE1_LOCAL_APP"] = "1"

        nodeProcess = pb.start()

        thread(name = "st-node-log") {
            try {
                BufferedReader(InputStreamReader(nodeProcess!!.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        Log.i(TAG, line)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "node log reader stopped", t)
            }
        }
    }

    private fun ensureSillyTavernConfig(workDir: File) {
        File(workDir, "data").mkdirs()
        val config = File(workDir, "config.yaml")
        if (!config.exists()) {
            config.writeText(
                """
                port: $port
                listen: false
                whitelistMode: false
                basicAuthMode: false
                dataRoot: ./data
                enableExtensions: true
                """.trimIndent() + "\n"
            )
        }
    }

    private fun waitUntilReady(): Boolean {
        repeat(80) { index ->
            if (isHttpReady()) return true
            if (index % 8 == 0) showStatus("等待本地酒馆启动中……${index / 2}s")
            Thread.sleep(500)
        }
        return false
    }

    private fun isHttpReady(): Boolean {
        return try {
            val connection = URL(localUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 350
            connection.readTimeout = 350
            connection.requestMethod = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code in 200..499
        } catch (_: Throwable) {
            false
        }
    }

    private fun copyAssetDir(assetPath: String, outDir: File) {
        val children = assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            outDir.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(outDir).use { output -> input.copyTo(output) }
            }
            return
        }
        outDir.mkdirs()
        children.forEach { name ->
            copyAssetDir("$assetPath/$name", File(outDir, name))
        }
    }

    private fun assetPathExists(path: String): Boolean {
        val parent = path.substringBeforeLast('/', "")
        val child = path.substringAfterLast('/')
        val list = assets.list(parent) ?: return false
        return list.contains(child)
    }

    private fun showStatus(text: String) {
        runOnUiThread {
            statusView.visibility = View.VISIBLE
            statusView.text = text
        }
    }

    private fun fail(text: String) {
        runOnUiThread {
            statusView.visibility = View.VISIBLE
            statusView.text = text
        }
    }

    private fun String.readTextOrNull(): String? = try { File(this).readText() } catch (_: Throwable) { null }
    private fun File.readTextOrNull(): String? = try { readText() } catch (_: Throwable) { null }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (isFinishing) {
            nodeProcess?.destroy()
            started = false
        }
        super.onDestroy()
    }
}
