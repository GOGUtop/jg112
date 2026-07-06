const http = require('http');
const port = 8000;

const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>今日新卡 Local - Node 测试成功</title>
  <style>
    body { margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif; background:linear-gradient(135deg,#f7fbff,#fff2fa); color:#252532; }
    .card { width:min(92vw,620px); padding:28px; border-radius:26px; background:rgba(255,255,255,.72); box-shadow:0 22px 80px rgba(88,108,140,.18); backdrop-filter:blur(20px); border:1px solid rgba(255,255,255,.7); }
    h1 { margin:0 0 12px; font-size:28px; }
    p { line-height:1.75; font-size:15px; }
    code { background:#f1f3f8; padding:3px 7px; border-radius:8px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Node 本地启动成功</h1>
    <p>这只是测试页，说明 APP 已经能在手机本地打开 <code>127.0.0.1:8000</code>。</p>
    <p>要打开真正的 SillyTavern，请把完整酒馆本体放进 <code>app/src/main/assets/sillytavern/</code>，并确保里面有 <code>server.js</code>、<code>public/</code>、<code>src/</code> 和 <code>node_modules/</code>。</p>
  </div>
</body>
</html>`;

http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
}).listen(port, '127.0.0.1', () => {
  console.log(`[stage1] test server running at http://127.0.0.1:${port}/`);
});
