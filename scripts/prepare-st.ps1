param(
    [Parameter(Mandatory = $true)]
    [string]$SillyTavernPath,

    [switch]$IncludeData
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$src = Resolve-Path $SillyTavernPath
$dest = Join-Path $root "app/src/main/assets/sillytavern"

if (!(Test-Path (Join-Path $src "server.js"))) {
    throw "这个目录不像 SillyTavern：找不到 server.js：$src"
}

if (!(Test-Path (Join-Path $src "node_modules"))) {
    Write-Host "警告：没看到 node_modules。请先在 SillyTavern 目录执行 npm install 或按官方方式安装依赖。" -ForegroundColor Yellow
}

if (Test-Path $dest) {
    Remove-Item $dest -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null

$excludeDirs = @(".git", ".github", "backups", "cache", "logs")
if (-not $IncludeData) {
    $excludeDirs += "data"
}
$excludeFiles = @(".env", "config.yaml")

$xd = $excludeDirs | ForEach-Object { "/XD", (Join-Path $src $_) }
$xf = $excludeFiles | ForEach-Object { "/XF", $_ }

Write-Host "正在复制 SillyTavern 到 Android assets..." -ForegroundColor Cyan
robocopy $src $dest /E /NFL /NDL /NJH /NJS /NP @xd @xf | Out-Null

# robocopy 0-7 都是成功或有差异
if ($LASTEXITCODE -gt 7) {
    throw "robocopy 失败，退出码：$LASTEXITCODE"
}

Write-Host "完成：$dest" -ForegroundColor Green
Write-Host "现在可以用 Android Studio 编译运行。" -ForegroundColor Green
