#!/bin/bash

set -e

DOMAIN=${1:-track.zhongsijie.cn}
APP_DIR="/opt/track"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
NGINX_CONF="$APP_DIR/nginx.conf"
NGINX_SITE_DIR="/etc/nginx/sites-available"
NGINX_ENABLE_DIR="/etc/nginx/sites-enabled"

echo "=========================================="
echo "开始部署 Track 应用"
echo "域名: $DOMAIN"
echo "=========================================="

# 创建应用目录
mkdir -p $APP_DIR
cd $APP_DIR

# 1. 检查并安装 Docker
echo "检查 Docker..."
if ! command -v docker &> /dev/null; then
    echo "安装 Docker..."
    apt-get update
    apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
    
    # 下载Docker GPG密钥（带重试）
    echo "下载 Docker GPG 密钥..."
    GPG_SUCCESS=false
    for i in {1..5}; do
        if curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg 2>/dev/null; then
            GPG_SUCCESS=true
            break
        fi
        echo "第 $i 次尝试失败，5秒后重试..."
        sleep 5
    done
    
    if [ "$GPG_SUCCESS" = false ]; then
        echo "警告: 无法从Docker官方源下载密钥，使用Ubuntu官方仓库安装Docker..."
        apt-get update
        apt-get install -y docker.io containerd
        systemctl enable docker
        systemctl start docker
        echo "Docker 安装完成（使用Ubuntu仓库版本）"
    else
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
        apt-get update
        apt-get install -y docker-ce docker-ce-cli containerd.io
        systemctl enable docker
        systemctl start docker
        echo "Docker 安装完成（使用Docker官方版本）"
    fi
else
    echo "Docker 已安装"
fi

# 2. 检查并安装 Docker Compose
echo "检查 Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "安装 Docker Compose..."
    COMPOSE_SUCCESS=false
    for i in {1..5}; do
        if curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose 2>/dev/null; then
            COMPOSE_SUCCESS=true
            break
        fi
        echo "第 $i 次尝试失败，5秒后重试..."
        sleep 5
    done
    
    if [ "$COMPOSE_SUCCESS" = false ]; then
        echo "警告: 无法从GitHub下载Docker Compose，尝试使用apt安装docker-compose-plugin..."
        # 如果Docker已安装，尝试安装docker-compose-plugin
        if command -v docker &> /dev/null; then
            apt-get update
            if apt-get install -y docker-compose-plugin 2>/dev/null; then
                # 创建docker-compose包装脚本
                cat > /usr/local/bin/docker-compose << 'COMPOSE_SCRIPT'
#!/bin/bash
docker compose "$@"
COMPOSE_SCRIPT
                chmod +x /usr/local/bin/docker-compose
                echo "Docker Compose 安装完成（使用docker-compose-plugin）"
            else
                echo "错误: 无法安装 Docker Compose"
                exit 1
            fi
        else
            echo "错误: Docker未安装，无法安装Docker Compose"
            exit 1
        fi
    else
        chmod +x /usr/local/bin/docker-compose
        echo "Docker Compose 安装完成（使用GitHub版本）"
    fi
else
    echo "Docker Compose 已安装"
fi

# 3. 检查并安装 Nginx
echo "检查 Nginx..."
if ! command -v nginx &> /dev/null; then
    echo "安装 Nginx..."
    apt-get update
    apt-get install -y nginx
    systemctl enable nginx
    systemctl start nginx
    echo "Nginx 安装完成"
else
    echo "Nginx 已安装"
fi

# 4. 加载 Docker 镜像
echo "加载 Docker 镜像..."
if [ -f "$APP_DIR/track-server.tar.gz" ]; then
    docker load < "$APP_DIR/track-server.tar.gz"
    echo "Docker 镜像加载完成"
else
    echo "警告: 未找到 Docker 镜像文件"
fi

# 5. 配置 SSL 证书
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

# 6. 配置 Nginx
echo "配置 Nginx..."
mkdir -p $NGINX_SITE_DIR $NGINX_ENABLE_DIR
cp $NGINX_CONF "$NGINX_SITE_DIR/$DOMAIN"
ln -sf "$NGINX_SITE_DIR/$DOMAIN" "$NGINX_ENABLE_DIR/$DOMAIN"

# 更新 Nginx 配置中的域名和SSL证书路径
sed -i "s/track.zhongsijie.cn/$DOMAIN/g" "$NGINX_SITE_DIR/$DOMAIN"

# 测试 Nginx 配置
nginx -t && systemctl reload nginx
echo "Nginx 配置完成"

# 7. 启动 Docker Compose 服务
echo "启动 Docker Compose 服务..."
cd $APP_DIR
docker-compose -f $COMPOSE_FILE down || true
docker-compose -f $COMPOSE_FILE up -d

# 8. 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose -f $COMPOSE_FILE ps

# 9. 配置 Certbot 自动续期
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
docker-compose -f $COMPOSE_FILE logs --tail=20 app

