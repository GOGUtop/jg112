#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "用法：bash scripts/prepare-st.sh /path/to/SillyTavern [--include-data]"
  exit 1
fi

SRC="$1"
INCLUDE_DATA="${2:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/sillytavern"

if [ ! -f "$SRC/server.js" ]; then
  echo "这个目录不像 SillyTavern：找不到 server.js：$SRC"
  exit 1
fi

if [ ! -d "$SRC/node_modules" ]; then
  echo "警告：没看到 node_modules。请先在 SillyTavern 目录执行 npm install 或按官方方式安装依赖。"
fi

rm -rf "$DEST"
mkdir -p "$DEST"

EXCLUDES=(--exclude='.git' --exclude='.github' --exclude='backups' --exclude='cache' --exclude='logs' --exclude='.env' --exclude='config.yaml')
if [ "$INCLUDE_DATA" != "--include-data" ]; then
  EXCLUDES+=(--exclude='data')
fi

echo "正在复制 SillyTavern 到 Android assets..."
rsync -a "${EXCLUDES[@]}" "$SRC/" "$DEST/"
echo "完成：$DEST"
echo "现在可以用 Android Studio 编译运行。"
