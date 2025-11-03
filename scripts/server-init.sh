#!/bin/bash

# Track 服务器初始化脚本
# 适用于全新的 Ubuntu 24.04 服务器
# 使用方法：bash scripts/server-init.sh

set -e

echo "================================"
echo "Track 服务器初始化脚本"
echo "Ubuntu 24.04 64位"
echo "================================"
echo ""

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "⚠️  建议使用 root 用户运行此脚本"
    echo "如果遇到权限问题，请使用：sudo bash scripts/server-init.sh"
    echo ""
fi

echo "📦 步骤 1/6: 更新系统软件包..."
apt update -y
apt upgrade -y

echo ""
echo "🐳 步骤 2/6: 安装 Docker..."
if command -v docker &> /dev/null; then
    echo "✅ Docker 已安装，跳过..."
else
    # 安装依赖
    apt install -y ca-certificates curl gnupg lsb-release
    
    # 下载并安装 Docker
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
    
    # 启动 Docker 服务
    systemctl start docker
    systemctl enable docker
    
    echo "✅ Docker 安装完成"
fi

echo ""
echo "🔧 步骤 3/6: 安装 Docker Compose..."
if docker compose version &> /dev/null; then
    echo "✅ Docker Compose 已安装，跳过..."
else
    apt install -y docker-compose-plugin
    echo "✅ Docker Compose 安装完成"
fi

echo ""
echo "🔥 步骤 4/6: 配置防火墙..."
if command -v ufw &> /dev/null; then
    # 允许 SSH
    ufw allow 22/tcp
    # 允许 HTTP
    ufw allow 80/tcp
    # 允许 HTTPS
    ufw allow 443/tcp
    # 允许应用端口
    ufw allow 8080/tcp
    
    # 启用防火墙（如果未启用）
    ufw --force enable
    
    echo "✅ 防火墙配置完成"
    ufw status
else
    echo "⚠️  未检测到 ufw 防火墙，跳过..."
fi

echo ""
echo "📝 步骤 5/6: 安装常用工具..."
apt install -y curl wget vim nano htop net-tools git

echo ""
echo "🔐 步骤 6/6: 创建非 root 用户（可选）..."
read -p "是否创建新用户？(yes/no，默认 no): " create_user
if [ "$create_user" = "yes" ]; then
    read -p "请输入用户名: " username
    adduser $username
    usermod -aG sudo $username
    usermod -aG docker $username
    echo "✅ 用户 $username 创建完成，并已添加到 sudo 和 docker 组"
fi

echo ""
echo "================================"
echo "初始化完成！"
echo "================================"
echo ""
echo "✅ 已安装："
docker --version
docker compose version
echo ""
echo "📝 下一步操作："
echo "1. 上传项目文件到服务器"
echo "2. 进入项目目录：cd ~/track"
echo "3. 配置环境变量：cp env.template .env && nano .env"
echo "4. 运行部署脚本：bash scripts/deploy.sh"
echo ""
echo "💡 提示："
echo "- 如果创建了新用户，建议退出后使用新用户登录"
echo "- 请记得在云服务器控制台开放安全组端口（80、443、8080）"
echo ""

