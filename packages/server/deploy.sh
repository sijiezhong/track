#!/bin/bash

set -e

DOMAIN=${1:-track.zhongsijie.cn}
APP_DIR="/opt/track"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
NGINX_CONF="$APP_DIR/nginx.conf"
NGINX_SITE_DIR="/etc/nginx/sites-available"
NGINX_ENABLE_DIR="/etc/nginx/sites-enabled"

# 选择可用的 Compose 命令
if docker compose version >/dev/null 2>&1; then
    DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DC="docker-compose"
else
    echo "错误: 未检测到 docker compose 或 docker-compose，请先安装 docker-compose-plugin 或 docker-compose。"
    exit 1
fi

echo "=========================================="
echo "开始部署 Track 应用"
echo "域名: $DOMAIN"
echo "=========================================="

# 创建应用目录
mkdir -p $APP_DIR
cd $APP_DIR

# 验证依赖是否已安装
echo "验证依赖..."
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装，请先运行依赖安装步骤"
    exit 1
fi
echo "✓ Docker 已安装: $(docker --version)"

# Compose 检查
if [ -n "$DC" ]; then
    echo "✓ Compose 可用: $($DC version 2>/dev/null || echo using $DC)"
fi

if ! command -v nginx &> /dev/null; then
    echo "错误: Nginx 未安装，请先运行依赖安装步骤"
    exit 1
fi
echo "✓ Nginx 已安装: $(nginx -v 2>&1)"

# 1. 加载 Docker 镜像
echo "加载 Docker 镜像..."
if [ -f "$APP_DIR/track-server.tar.gz" ]; then
    docker load < "$APP_DIR/track-server.tar.gz"
    echo "Docker 镜像加载完成"
else
    echo "警告: 未找到 Docker 镜像文件"
fi

# 2. 配置 SSL 证书
echo "配置 SSL 证书..."
if ! command -v certbot &> /dev/null; then
    echo "安装 Certbot..."
    apt-get update
    apt-get install -y certbot python3-certbot-nginx
fi

# 检查证书是否存在
if [ ! -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    echo "获取 SSL 证书..."
    # 首先创建临时 Nginx 配置以获取证书
    mkdir -p $NGINX_SITE_DIR $NGINX_ENABLE_DIR
    cat > "$NGINX_SITE_DIR/$DOMAIN" << EOF
server {
    listen 80;
    server_name $DOMAIN;
    
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
    
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}
EOF
    ln -sf "$NGINX_SITE_DIR/$DOMAIN" "$NGINX_ENABLE_DIR/$DOMAIN"
    nginx -t && systemctl reload nginx
    
    # 获取证书（使用域名邮箱，如果失败可以稍后手动配置）
    CERTBOT_EMAIL=${CERTBOT_EMAIL:-"admin@$DOMAIN"}
    certbot certonly --nginx -d $DOMAIN --non-interactive --agree-tos --email $CERTBOT_EMAIL --redirect || {
        echo "警告: 自动获取证书失败，请稍后手动执行:"
        echo "certbot certonly --nginx -d $DOMAIN"
    }
else
    echo "SSL 证书已存在"
fi

# 3. 配置 Nginx
echo "配置 Nginx..."
mkdir -p $NGINX_SITE_DIR $NGINX_ENABLE_DIR
cp $NGINX_CONF "$NGINX_SITE_DIR/$DOMAIN"
ln -sf "$NGINX_SITE_DIR/$DOMAIN" "$NGINX_ENABLE_DIR/$DOMAIN"

# 更新 Nginx 配置中的域名和SSL证书路径
sed -i "s/track.zhongsijie.cn/$DOMAIN/g" "$NGINX_SITE_DIR/$DOMAIN"

# 测试 Nginx 配置
nginx -t && systemctl reload nginx
echo "Nginx 配置完成"

# 4. 启动 Docker Compose 服务
echo "启动 Docker Compose 服务..."
cd $APP_DIR
$DC -f $COMPOSE_FILE down || true
$DC -f $COMPOSE_FILE up -d

# 5. 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
$DC -f $COMPOSE_FILE ps

# 6. 配置 Certbot 自动续期
echo "配置 Certbot 自动续期..."
if ! systemctl list-units --type=timer | grep -q certbot.timer; then
    systemctl enable certbot.timer
    systemctl start certbot.timer
fi

echo "=========================================="
echo "部署完成！"
echo "应用地址: https://$DOMAIN"
echo "=========================================="

# 显示服务日志
echo "应用日志 (最后20行):"
$DC -f $COMPOSE_FILE logs --tail=20 app

