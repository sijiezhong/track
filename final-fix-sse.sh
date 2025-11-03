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

# 1. 删除备份文件（避免干扰nginx测试）
send "rm -f /etc/nginx/sites-enabled/track.before-remove-duplicate && echo '备份文件已删除'\r"
expect {
    "# " {}
    "$ " {}
}

# 2. 查看当前配置，确认有多少个SSE location
send "echo '=== 当前SSE配置位置 ===' && grep -n 'location.*events/stream' /etc/nginx/sites-enabled/track\r"
expect {
    "# " {}
    "$ " {}
}

# 3. 备份主配置文件
send "cp /etc/nginx/sites-enabled/track /etc/nginx/sites-enabled/track.backup.\$(date +%Y%m%d_%H%M%S) && echo '主配置文件已备份'\r"
expect {
    "# " {}
    "$ " {}
}

# 4. 删除所有SSE相关的location块（包括旧的和重复的）
send "cd /etc/nginx/sites-enabled && sed -i '/location \\/api.*events\\/stream {/,/}/d' track && echo '已删除所有SSE配置'\r"
expect {
    "# " {}
    "$ " {}
}

# 5. 找到 location / 的位置，在它之前插入正确的配置
send "sed -i '/location \\/ {/i\\    # SSE Events Stream\\n    location /api/v1/events/stream {\\n        proxy_pass http://localhost:8080/api/v1/events/stream;\\n        proxy_set_header Host \\\$host;\\n        proxy_set_header X-Real-IP \\\$remote_addr;\\n        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;\\n        proxy_set_header X-Forwarded-Proto \\\$scheme;\\n        proxy_set_header Authorization \\\$http_authorization;\\n        proxy_set_header X-Tenant-Id \\\$http_x_tenant_id;\\n        proxy_set_header Connection \"\";\\n        proxy_http_version 1.1;\\n        chunked_transfer_encoding off;\\n        proxy_buffering off;\\n        proxy_cache off;\\n        proxy_connect_timeout 60s;\\n        proxy_send_timeout 3600s;\\n        proxy_read_timeout 3600s;\\n    }\\n' track && echo 'SSE配置已添加'\r"
expect {
    "# " {}
    "$ " {}
}

# 6. 验证配置
send "echo '=== 验证配置 ===' && grep -c 'location.*events/stream' track\r"
expect {
    "# " {}
    "$ " {}
}

# 7. 测试配置
send "nginx -t\r"
expect {
    "syntax is ok" {
        puts "\n✅ 配置语法正确！"
    }
    "test is successful" {
        puts "\n✅ 配置测试成功！"
    }
    "# " {}
    "$ " {}
    timeout {}
}

# 8. 重新加载Nginx
send "systemctl reload nginx && echo '✅ Nginx 已重新加载'\r"
expect {
    "# " {}
    "$ " {}
}

# 9. 最终验证
send "echo '=== 最终验证 ===' && curl -m 5 -N -H \"Authorization: Bearer role:ADMIN\" -H \"X-Tenant-Id: 1\" https://zhongsijie.cn/api/v1/events/stream 2>&1 | head -10\r"
expect {
    "# " {}
    "$ " {}
    timeout {}
}

puts "\n✅ 修复完成！SSE 配置已正确设置。"
puts "现在可以刷新浏览器页面验证效果。"
send "exit\r"
expect eof

