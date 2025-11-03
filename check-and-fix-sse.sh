#!/usr/bin/expect -f

set timeout 30
set server "47.113.180.87"
set user "root"
set password "yiersan123."

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
}

# 查看当前配置
send "echo '=== 当前配置中的 SSE location ===' && grep -n 'location.*events/stream' /etc/nginx/sites-enabled/track\r"
expect {
    "# " {}
    "$ " {}
}

# 检查配置数量
send "grep -c 'location.*events/stream' /etc/nginx/sites-enabled/track\r"
expect {
    "# " {}
    "$ " {}
}

# 清理重复配置：只保留第一个正确的配置
send "cd /etc/nginx/sites-enabled && cp track track.before-remove-duplicate && sed -i '/location \\/api\\/v1\\/events\\/stream {/,/}/d' track && echo '已删除重复配置'\r"
expect {
    "# " {}
    "$ " {}
}

# 检查 location / 的位置
send "grep -n 'location /' track | head -1\r"
expect {
    "# " {}
    "$ " {}
}

# 在 location / 之前插入正确的配置
send "cat >> /tmp/sse-config.txt << 'EOF'
    # SSE Events Stream
    location /api/v1/events/stream {
        proxy_pass http://localhost:8080/api/v1/events/stream;
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;
        proxy_set_header Authorization \\\$http_authorization;
        proxy_set_header X-Tenant-Id \\\$http_x_tenant_id;
        proxy_set_header Connection \"\";
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
        proxy_buffering off;
        proxy_cache off;
        proxy_connect_timeout 60s;
        proxy_send_timeout 3600s;
        proxy_read_timeout 3600s;
    }

EOF
sed -i '/location \\/ {/r /tmp/sse-config.txt' track && echo '配置已添加'\r"
expect {
    "# " {}
    "$ " {}
}

# 测试配置
send "nginx -t\r"
expect {
    "syntax is ok" {
        puts "配置语法正确！"
    }
    "# " {}
    "$ " {}
}

# 重新加载
send "systemctl reload nginx && echo '✅ Nginx 已重新加载'\r"
expect {
    "# " {}
    "$ " {}
}

# 验证
send "curl -m 5 -N -H \"Authorization: Bearer role:ADMIN\" -H \"X-Tenant-Id: 1\" https://zhongsijie.cn/api/v1/events/stream 2>&1 | head -5\r"
expect {
    "# " {}
    "$ " {}
    timeout {}
}

puts "\n修复完成！"
send "exit\r"
expect eof

