#!/usr/bin/expect -f

# 自动修复 SSE Nginx 配置脚本
# 使用 expect 自动输入密码

set timeout 30
set server "47.113.180.87"
set user "root"
set password "yiersan123."

# 构建修复命令
set fix_cmd "
cd /etc/nginx/sites-available && \
cp track track.backup.\$(date +%Y%m%d_%H%M%S) && \
sed -i '/location \\/ {/i\\
    # SSE Events Stream\\
    location /api/v1/events/stream {\\
        proxy_pass http://localhost:8080/api/v1/events/stream;\\
        proxy_set_header Host \$host;\\
        proxy_set_header X-Real-IP \$remote_addr;\\
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;\\
        proxy_set_header X-Forwarded-Proto \$scheme;\\
        proxy_set_header Authorization \$http_authorization;\\
        proxy_set_header X-Tenant-Id \$http_x_tenant_id;\\
        proxy_set_header Connection \"\";\\
        proxy_http_version 1.1;\\
        chunked_transfer_encoding off;\\
        proxy_buffering off;\\
        proxy_cache off;\\
        proxy_connect_timeout 60s;\\
        proxy_send_timeout 3600s;\\
        proxy_read_timeout 3600s;\\
    }\\
' track && \
nginx -t && \
systemctl reload nginx && \
echo \"\" && \
echo \"✅ 修复成功！\" && \
echo \"\" && \
echo \"验证修复：\" && \
curl -m 3 -N -H \"Authorization: Bearer role:ADMIN\" -H \"X-Tenant-Id: 1\" https://zhongsijie.cn/api/v1/events/stream 2>&1 | head -3
"

# 连接服务器
spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $user@$server

# 处理可能的密码提示或主机密钥确认
expect {
    "yes/no" {
        send "yes\r"
        exp_continue
    }
    "password:" {
        send "$password\r"
    }
    "Password:" {
        send "$password\r"
    }
    "Are you sure you want to continue connecting" {
        send "yes\r"
        exp_continue
    }
}

# 等待命令行提示符
expect {
    "# " {
        send "$fix_cmd\r"
    }
    "$ " {
        send "$fix_cmd\r"
    }
    timeout {
        puts "连接超时"
        exit 1
    }
}

# 等待命令执行完成
expect {
    "# " {
        interact
    }
    "$ " {
        interact
    }
    timeout {
        puts "命令执行超时"
        exit 1
    }
}

exit 0

