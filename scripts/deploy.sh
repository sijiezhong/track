#!/bin/bash

# Track 后端服务一键部署脚本
# 使用方法：bash scripts/deploy.sh

set -e

echo "================================"
echo "Track 后端服务部署脚本"
echo "================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：未检测到 Docker，请先安装 Docker"
    echo "安装命令：curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! docker compose version &> /dev/null; then
    echo "❌ 错误：未检测到 Docker Compose，请先安装"
    echo "安装命令：sudo apt install docker-compose-plugin -y"
    exit 1
fi

echo "✅ Docker 环境检查通过"
echo ""

# 检查 .env 文件是否存在
if [ ! -f ".env" ]; then
    echo "⚠️  警告：未找到 .env 文件"
    echo "正在从模板创建 .env 文件..."
    if [ -f "env.template" ]; then
        cp env.template .env
        echo "✅ 已创建 .env 文件，请编辑此文件并设置正确的配置"
        echo "特别是数据库密码: POSTGRES_PASSWORD"
        echo ""
        read -p "按 Enter 键继续部署，或 Ctrl+C 取消..." 
    else
        echo "❌ 错误：未找到 env.template 文件"
        exit 1
    fi
fi

echo "================================"
echo "开始部署..."
echo "================================"
echo ""

# 停止旧容器
echo "🛑 停止现有容器..."
docker compose down

# 构建并启动服务
echo ""
echo "🚀 构建并启动服务（这可能需要几分钟）..."
docker compose up -d --build

# 等待服务启动
echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "📊 检查服务状态..."
docker compose ps

# 检查健康状态
echo ""
echo "🏥 检查服务健康状态..."
sleep 5

if curl -f http://localhost:8080/actuator/health &> /dev/null; then
    echo "✅ 服务健康检查通过！"
else
    echo "⚠️  警告：服务健康检查失败，请查看日志"
    echo "查看日志命令：docker compose logs server"
fi

echo ""
echo "================================"
echo "部署完成！"
echo "================================"
echo ""
echo "📍 服务访问地址："
echo "   - 健康检查：http://YOUR_SERVER_IP:8080/actuator/health"
echo "   - API 文档：http://YOUR_SERVER_IP:8080/swagger-ui.html"
echo ""
echo "📝 常用命令："
echo "   - 查看日志：docker compose logs -f server"
echo "   - 重启服务：docker compose restart"
echo "   - 停止服务：docker compose stop"
echo "   - 查看状态：docker compose ps"
echo ""

