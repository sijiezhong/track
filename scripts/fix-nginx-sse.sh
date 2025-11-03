#!/bin/bash

# SSE Nginx 配置修复脚本
# 用于快速修复生产环境的 Nginx 配置，解决 SSE 路径不匹配问题

set -e

echo "=========================================="
echo "  SSE Nginx 配置修复脚本"
echo "=========================================="
echo ""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否提供了服务器地址
if [ -z "$1" ]; then
    echo -e "${YELLOW}用法：$0 <服务器地址>${NC}"
    echo ""
    echo "示例："
    echo "  $0 zhongsijie.cn"
    echo "  $0 user@zhongsijie.cn"
    echo ""
    exit 1
fi

SERVER="$1"

echo -e "${YELLOW}目标服务器：${NC}$SERVER"
echo ""

# 确认操作
read -p "是否继续？[y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo -e "${GREEN}步骤 1/4: 检查 Nginx 配置文件...${NC}"

# 检查配置文件是否存在
if ssh "$SERVER" "test -f /etc/nginx/sites-available/track"; then
    echo "✓ 找到配置文件"
else
    echo -e "${RED}✗ 配置文件不存在：/etc/nginx/sites-available/track${NC}"
    echo "  请先创建基础配置文件"
    exit 1
fi

echo ""
echo -e "${GREEN}步骤 2/4: 备份当前配置...${NC}"

ssh "$SERVER" "sudo cp /etc/nginx/sites-available/track /etc/nginx/sites-available/track.backup.$(date +%Y%m%d_%H%M%S)"
echo "✓ 配置已备份"

echo ""
echo -e "${GREEN}步骤 3/4: 检查配置中的 SSE 路径...${NC}"

# 检查是否有错误的配置
if ssh "$SERVER" "grep -q 'location /api/events/stream' /etc/nginx/sites-available/track"; then
    echo -e "${YELLOW}⚠ 发现旧路径配置 /api/events/stream${NC}"
    
    # 检查是否有正确的配置
    if ssh "$SERVER" "grep -q 'location /api/v1/events/stream' /etc/nginx/sites-available/track"; then
        echo "✓ 已有正确配置 /api/v1/events/stream"
        echo "  建议：手动检查并移除旧配置"
    else
        echo -e "${RED}✗ 缺少正确的 /api/v1/events/stream 配置${NC}"
        echo ""
        echo "需要手动修复，请参考以下配置："
        echo ""
        cat <<'EOF'
location /api/v1/events/stream {
    proxy_pass http://localhost:8080/api/v1/events/stream;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Authorization $http_authorization;
    proxy_set_header X-Tenant-Id $http_x_tenant_id;
    
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
    proxy_buffering off;
    proxy_cache off;
    
    proxy_connect_timeout 60s;
    proxy_send_timeout 3600s;
    proxy_read_timeout 3600s;
}
EOF
        echo ""
        echo "要立即编辑配置吗？"
        read -p "[y/N] " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            ssh -t "$SERVER" "sudo nano /etc/nginx/sites-available/track"
        else
            echo "请手动SSH到服务器并编辑配置："
            echo "  ssh $SERVER"
            echo "  sudo nano /etc/nginx/sites-available/track"
            exit 0
        fi
    fi
else
    echo -e "${YELLOW}⚠ 未找到任何 SSE 配置${NC}"
    echo "需要添加 SSE 配置，请参考 FIX_SSE_NGINX.md"
    exit 1
fi

echo ""
echo -e "${GREEN}步骤 4/4: 测试并重启 Nginx...${NC}"

# 测试配置
echo "测试 Nginx 配置语法..."
if ssh "$SERVER" "sudo nginx -t"; then
    echo "✓ 配置语法正确"
else
    echo -e "${RED}✗ 配置语法错误！${NC}"
    echo "  配置未应用，原配置保持不变"
    exit 1
fi

# 重启 Nginx
echo ""
read -p "配置正确，是否重启 Nginx？[y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    ssh "$SERVER" "sudo systemctl reload nginx"
    echo -e "${GREEN}✓ Nginx 已重新加载${NC}"
else
    echo "Nginx 未重启，配置未生效"
    exit 0
fi

echo ""
echo -e "${GREEN}=========================================="
echo "  修复完成！"
echo "==========================================${NC}"
echo ""
echo "接下来："
echo "1. 刷新浏览器页面"
echo "2. 打开开发者工具控制台"
echo "3. 查看是否有 [SSE] ✅ 连接成功 的日志"
echo ""
echo "如果仍有问题，请查看："
echo "  - Nginx 错误日志：ssh $SERVER sudo tail -100 /var/log/nginx/error.log"
echo "  - 后端日志：ssh $SERVER sudo journalctl -u track.service -f"
echo ""

