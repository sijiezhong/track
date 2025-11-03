#!/usr/bin/expect -f

set timeout 30
set server "47.113.180.87"
set user "root"
set password "yiersan123."

# 先备份，然后创建修复脚本
set backup_cmd "cd /etc/nginx/sites-available && cp track track.backup.\$(date +%Y%m%d_%H%M%S) && echo '备份完成'"

# 创建修复配置脚本
set create_script_cmd "cat > /tmp/sse-fix.sh << 'EOF'
#!/bin/bash
cd /etc/nginx/sites-available
# 在 location / 之前插入 SSE 配置
sed -i '/location \\/ {/i\\
    # SSE Events Stream\\
    location /api/v1/events/stream {\\
        proxy_pass http://localhost:8080/api/v1/events/stream;\\
        proxy_set_header Host \\\$host;\\
        proxy_set_header X-Real-IP \\\$remote_addr;\\
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;\\
        proxy_set_header X-Forwarded-Proto \\\$scheme;\\
        proxy_set_header Authorization \\\$http_authorization;\\
        proxy_set_header X-Tenant-Id \\\$http_x_tenant_id;\\
        proxy_set_header Connection \"\";\\
        proxy_http_version 1.1;\\
        chunked_transfer_encoding off;\\
        proxy_buffering off;\\
        proxy_cache off;\\
        proxy_connect_timeout 60s;\\
        proxy_send_timeout 3600s;\\
        proxy_read_timeout 3600s;\\
    }\\
' track
EOF
chmod +x /tmp/sse-fix.sh"

spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $user@$server

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
}

expect {
    "# " {}
    "$ " {}
    timeout {
        puts "等待提示符超时"
        exit 1
    }
}

# 执行备份
send "$backup_cmd\r"
expect {
    "# " {}
    "$ " {}
}

# 创建修复脚本
send "$create_script_cmd\r"
expect {
    "# " {}
    "$ " {}
}

# 执行修复脚本
send "/tmp/sse-fix.sh\r"
expect {
    "# " {}
    "$ " {}
}

# 测试配置
send "nginx -t\r"
expect {
    "syntax is ok" {
        puts "配置语法正确"
    }
    "# " {}
    "$ " {}
}

# 重新加载 Nginx
send "systemctl reload nginx\r"
expect {
    "# " {}
    "$ " {}
}

# 验证修复
send "curl -m 3 -N -H \"Authorization: Bearer role:ADMIN\" -H \"X-Tenant-Id: 1\" https://zhongsijie.cn/api/v1/events/stream 2>&1 | head -3\r"
expect {
    "# " {}
    "$ " {}
    timeout {}
}

puts "\n修复完成！请查看上面的输出结果。"
send "exit\r"
expect eof

