#!/bin/bash

# Track 项目上传脚本（从 macOS 上传到服务器）
# 使用方法：bash scripts/upload-to-server.sh <服务器IP> <用户名>
# 示例：bash scripts/upload-to-server.sh 123.45.67.89 root

set -e

echo "================================"
echo "Track 项目上传脚本"
echo "================================"
echo ""

# 检查参数
if [ -z "$1" ] || [ -z "$2" ]; then
    echo "❌ 错误：缺少参数"
    echo "使用方法：bash scripts/upload-to-server.sh <服务器IP> <用户名>"
    echo "示例：bash scripts/upload-to-server.sh 123.45.67.89 root"
    exit 1
fi

SERVER_IP=$1
USERNAME=$2
REMOTE_DIR="~/track"

echo "📋 上传配置："
echo "   服务器 IP：$SERVER_IP"
echo "   用户名：$USERNAME"
echo "   目标目录：$REMOTE_DIR"
echo ""

# 确认上传
read -p "确认要上传吗？(yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "取消上传"
    exit 0
fi

echo ""
echo "📦 准备上传文件..."

# 创建临时目录
TEMP_DIR=$(mktemp -d)
echo "创建临时目录：$TEMP_DIR"

# 复制必要文件
echo "复制必要文件到临时目录..."
mkdir -p "$TEMP_DIR/track"
cp -r server "$TEMP_DIR/track/"
cp docker-compose.yml "$TEMP_DIR/track/"
cp env.template "$TEMP_DIR/track/"
cp nginx.conf.example "$TEMP_DIR/track/"
cp -r scripts "$TEMP_DIR/track/"
cp 部署指南.md "$TEMP_DIR/track/" 2>/dev/null || true
cp DEPLOYMENT.md "$TEMP_DIR/track/" 2>/dev/null || true
cp README.md "$TEMP_DIR/track/"

echo "✅ 文件准备完成"
echo ""

# 上传文件
echo "🚀 开始上传到服务器..."
scp -r "$TEMP_DIR/track" $USERNAME@$SERVER_IP:~/

if [ $? -eq 0 ]; then
    echo "✅ 上传成功！"
else
    echo "❌ 上传失败"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# 清理临时目录
echo ""
echo "🧹 清理临时文件..."
rm -rf "$TEMP_DIR"

echo ""
echo "================================"
echo "上传完成！"
echo "================================"
echo ""
echo "📝 下一步操作（在服务器上执行）："
echo ""
echo "1. 连接到服务器："
echo "   ssh $USERNAME@$SERVER_IP"
echo ""
echo "2. 进入项目目录："
echo "   cd ~/track"
echo ""
echo "3. 初始化服务器（仅首次）："
echo "   sudo bash scripts/server-init.sh"
echo ""
echo "4. 配置环境变量："
echo "   cp env.template .env"
echo "   nano .env  # 修改数据库密码"
echo ""
echo "5. 部署服务："
echo "   bash scripts/deploy.sh"
echo ""
echo "6. 检查状态："
echo "   bash scripts/check-status.sh"
echo ""

