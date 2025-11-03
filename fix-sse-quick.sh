#!/bin/bash
# 快速修复 SSE 配置脚本 - 在服务器上执行

echo "======================================"
echo "  修复 Nginx SSE 配置"
echo "======================================"
echo ""

# 检查是否是 root 或有 sudo 权限
if [ "$EUID" -ne 0 ]; then 
    SUDO="sudo"
else
    SUDO=""
fi

# 1. 备份配置
echo "1. 备份当前配置..."
$SUDO cp /etc/nginx/sites-available/track /etc/nginx/sites-available/track.backup.$(date +%Y%m%d_%H%M%S)
echo "✓ 已备份"
echo ""

# 2. 检查当前配置
echo "2. 检查当前配置..."
if grep -q "location /api/v1/events/stream" /etc/nginx/sites-available/track; then
    echo "✓ 配置中已有 /api/v1/events/stream 路径"
    echo "  可能已经修复过了，继续检查配置详情..."
else
    echo "⚠ 配置中缺少 /api/v1/events/stream 路径"
    echo "  需要添加配置"
fi
echo ""

# 3. 创建修复配置片段
echo "3. 准备配置片段..."
cat > /tmp/sse-config.txt <<'EOF'

    # SSE (Server-Sent Events) 特殊配置
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

echo "✓ 配置片段已准备"
echo ""
echo "配置内容："
cat /tmp/sse-config.txt
echo ""

# 4. 指导手动修改
echo "======================================"
echo "  手动修改步骤"
echo "======================================"
echo ""
echo "请执行以下命令编辑配置文件："
echo "  sudo nano /etc/nginx/sites-available/track"
echo ""
echo "在 'server {' 块中，找到 'location /' 的位置，"
echo "在它之前添加上面的 SSE 配置。"
echo ""
echo "保存后，执行："
echo "  sudo nginx -t          # 测试配置"
echo "  sudo systemctl reload nginx  # 重新加载"
echo ""

# 提供自动修复选项
read -p "是否尝试自动在配置文件中添加（需要仔细检查）？[y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "正在尝试自动添加配置..."
    
    # 检查是否已存在
    if grep -q "location /api/v1/events/stream" /etc/nginx/sites-available/track; then
        echo "⚠ 配置已存在，跳过添加"
    else
        # 在第一个 location / 之前插入
        $SUDO sed -i.bak '/location \/ {/i\    # SSE (Server-Sent Events) 特殊配置\n    location /api/v1/events/stream {\n        proxy_pass http://localhost:8080/api/v1/events/stream;\n        proxy_set_header Host $host;\n        proxy_set_header X-Real-IP $remote_addr;\n        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_set_header Authorization $http_authorization;\n        proxy_set_header X-Tenant-Id $http_x_tenant_id;\n        \n        proxy_set_header Connection '"''"';\n        proxy_http_version 1.1;\n        chunked_transfer_encoding off;\n        proxy_buffering off;\n        proxy_cache off;\n        \n        proxy_connect_timeout 60s;\n        proxy_send_timeout 3600s;\n        proxy_read_timeout 3600s;\n    }\n' /etc/nginx/sites-available/track
        
        echo "✓ 配置已添加"
    fi
    
    echo ""
    echo "4. 测试配置..."
    if $SUDO nginx -t; then
        echo "✓ 配置语法正确"
        echo ""
        read -p "是否重新加载 Nginx？[y/N] " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            $SUDO systemctl reload nginx
            echo "✓ Nginx 已重新加载"
        fi
    else
        echo "✗ 配置语法错误！"
        echo "  正在恢复备份..."
        $SUDO cp /etc/nginx/sites-available/track.bak /etc/nginx/sites-available/track
        echo "  配置已恢复"
    fi
fi

echo ""
echo "======================================"
echo "  验证修复"
echo "======================================"
echo ""
echo "测试 SSE 端点："
echo "  curl -N -H 'Authorization: Bearer role:ADMIN' \\"
echo "       -H 'X-Tenant-Id: 1' \\"
echo "       https://zhongsijie.cn/api/v1/events/stream"
echo ""
echo "应该立即看到："
echo "  event: init"
echo "  data: ok"
echo ""

