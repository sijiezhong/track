#!/bin/bash

# Track 服务状态检查脚本
# 使用方法：bash scripts/check-status.sh

echo "================================"
echo "Track 服务状态检查"
echo "================================"
echo ""

# 检查 Docker 服务
echo "🐳 Docker 服务状态："
if systemctl is-active --quiet docker; then
    echo "✅ Docker 服务运行中"
else
    echo "❌ Docker 服务未运行"
fi
echo ""

# 检查容器状态
echo "📦 容器状态："
docker compose ps
echo ""

# 检查应用健康状态
echo "🏥 应用健康检查："
if curl -f http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
    echo "✅ 应用健康状态：正常"
    curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || cat
else
    echo "❌ 应用健康检查失败"
fi
echo ""

# 检查端口占用
echo "🔌 端口占用情况："
echo "PostgreSQL (5432):"
if netstat -tuln 2>/dev/null | grep -q ":5432 " || ss -tuln 2>/dev/null | grep -q ":5432 "; then
    echo "  ✅ 端口 5432 已监听"
else
    echo "  ❌ 端口 5432 未监听"
fi

echo "Redis (6379):"
if netstat -tuln 2>/dev/null | grep -q ":6379 " || ss -tuln 2>/dev/null | grep -q ":6379 "; then
    echo "  ✅ 端口 6379 已监听"
else
    echo "  ❌ 端口 6379 未监听"
fi

echo "应用服务 (8080):"
if netstat -tuln 2>/dev/null | grep -q ":8080 " || ss -tuln 2>/dev/null | grep -q ":8080 "; then
    echo "  ✅ 端口 8080 已监听"
else
    echo "  ❌ 端口 8080 未监听"
fi
echo ""

# 检查磁盘使用情况
echo "💾 磁盘使用情况："
df -h / | tail -1
echo ""

# 检查内存使用情况
echo "🧠 内存使用情况："
free -h | grep Mem
echo ""

# 检查 Docker 资源使用
echo "📊 Docker 资源使用："
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null || echo "无法获取 Docker 统计信息"
echo ""

# 检查最近的日志（最后 10 行）
echo "📝 最近的应用日志："
docker compose logs --tail=10 server 2>/dev/null || echo "无法获取日志"
echo ""

echo "================================"
echo "检查完成"
echo "================================"
echo ""
echo "💡 常用命令："
echo "  查看详细日志：docker compose logs -f server"
echo "  重启服务：docker compose restart"
echo "  进入容器：docker compose exec server bash"

