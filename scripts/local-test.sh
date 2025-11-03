#!/bin/bash

# Track 本地测试脚本（macOS）
# 使用方法：bash scripts/local-test.sh

set -e

echo "================================"
echo "Track 本地部署测试"
echo "================================"
echo ""

# 检查 Docker Desktop 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker Desktop 未运行"
    echo "请先启动 Docker Desktop"
    exit 1
fi

echo "✅ Docker Desktop 运行中"
echo ""

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "⚠️  未找到 .env 文件，从模板创建..."
    if [ -f "env.template" ]; then
        cp env.template .env
        echo "✅ 已创建 .env 文件"
    else
        echo "❌ 未找到 env.template 文件"
        exit 1
    fi
fi

echo "================================"
echo "开始本地部署测试..."
echo "================================"
echo ""

# 停止已有容器
echo "🛑 清理已有容器..."
docker compose down -v

# 构建并启动
echo ""
echo "🚀 构建并启动服务..."
docker compose up -d --build

# 等待服务启动
echo ""
echo "⏳ 等待服务启动（最多等待 2 分钟）..."
for i in {1..24}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 服务启动成功！（耗时 ${i}0 秒）"
        break
    fi
    if [ $i -eq 24 ]; then
        echo "❌ 服务启动超时，请查看日志"
        docker compose logs server
        exit 1
    fi
    echo -n "."
    sleep 5
done

echo ""
echo ""
echo "================================"
echo "测试部署成功！"
echo "================================"
echo ""
echo "📊 容器状态："
docker compose ps
echo ""
echo "🏥 健康检查："
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
echo ""
echo "🌐 访问地址："
echo "   - API 文档：http://localhost:8080/swagger-ui.html"
echo "   - 健康检查：http://localhost:8080/actuator/health"
echo ""
echo "📝 测试命令："
echo "   - 查看日志：docker compose logs -f server"
echo "   - 停止服务：docker compose down"
echo ""

