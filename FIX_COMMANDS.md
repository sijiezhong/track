# 🚀 一键修复 SSE 问题

## 最简单的方法（复制粘贴执行）

### 步骤 1：SSH 登录服务器

在你的终端运行：
```bash
ssh zhongsijie.cn
```

### 步骤 2：执行一键修复命令

登录后，复制以下**整个命令块**并粘贴执行：

```bash
# 一键修复 Nginx SSE 配置
sudo cp /etc/nginx/sites-available/track /etc/nginx/sites-available/track.backup.$(date +%Y%m%d_%H%M%S) && \
sudo sed -i '/location \/ {/i\    # SSE Events Stream\n    location /api/v1/events/stream {\n        proxy_pass http://localhost:8080/api/v1/events/stream;\n        proxy_set_header Host $host;\n        proxy_set_header X-Real-IP $remote_addr;\n        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_set_header Authorization $http_authorization;\n        proxy_set_header X-Tenant-Id $http_x_tenant_id;\n        proxy_set_header Connection '"''"';\n        proxy_http_version 1.1;\n        chunked_transfer_encoding off;\n        proxy_buffering off;\n        proxy_cache off;\n        proxy_connect_timeout 60s;\n        proxy_send_timeout 3600s;\n        proxy_read_timeout 3600s;\n    }\n' /etc/nginx/sites-available/track && \
sudo nginx -t && \
sudo systemctl reload nginx && \
echo "" && \
echo "✅ 修复成功！" && \
echo "" && \
echo "验证修复：" && \
curl -m 3 -N -H "Authorization: Bearer role:ADMIN" -H "X-Tenant-Id: 1" https://zhongsijie.cn/api/v1/events/stream 2>&1 | head -3
```

### 步骤 3：查看结果

如果看到：
```
✅ 修复成功！

验证修复：
event: init
data: ok
```

说明修复成功！

---

## 如果上面的命令报错（配置已存在）

如果提示配置已存在，说明可能已经有 SSE 配置了。检查配置：

```bash
# 查看当前配置
grep -A 15 "location /api/v1/events/stream" /etc/nginx/sites-available/track
```

如果配置存在但不正确，手动编辑：
```bash
sudo nano /etc/nginx/sites-available/track
```

确保有以下配置块（在 `location /` 之前）：

```nginx
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
```

保存后（Ctrl+O, Enter, Ctrl+X），运行：
```bash
sudo nginx -t && sudo systemctl reload nginx
```

---

## 方案 B：使用自动化脚本

如果你想要更详细的交互式修复：

```bash
# 在本地运行（会帮你SSH到服务器）
cd /Users/zhongsijie/code/track
./scripts/fix-nginx-sse.sh zhongsijie.cn
```

或者在服务器上运行：
```bash
# SSH 登录后
cd /tmp
wget https://raw.githubusercontent.com/your-repo/track/main/fix-sse-quick.sh
chmod +x fix-sse-quick.sh
./fix-sse-quick.sh
```

---

## 验证修复

### 1. 测试 SSE 端点
```bash
curl -N -H "Authorization: Bearer role:ADMIN" \
     -H "X-Tenant-Id: 1" \
     https://zhongsijie.cn/api/v1/events/stream
```

应该立即看到：
```
event: init
data: ok

（连接保持）
```

按 Ctrl+C 退出。

### 2. 前端验证

1. 刷新浏览器页面
2. 打开控制台（F12）
3. 应该看到：
```
[SSE] 🔄 准备连接事件流 | URL: /api/v1/events/stream | tenantId: 1 | hasToken: true
[SSE] 📡 正在发起 SSE 连接...
[SSE] 📨 收到响应 | status: 200 | contentType: text/event-stream
[SSE] ✅ 连接成功！正在等待事件数据...
```

### 3. 触发测试事件

访问诊断页面：`http://localhost:5173/sse-diagnostic`
- 点击 "2. 测试 SSE 连接"
- 点击 "3. 发送测试事件"
- 应该立即看到新事件

---

## 常见问题

### Q: 如何回滚？
```bash
# 查看备份
ls -la /etc/nginx/sites-available/track.backup*

# 恢复最新的备份
sudo cp /etc/nginx/sites-available/track.backup.XXXXXX /etc/nginx/sites-available/track
sudo nginx -t && sudo systemctl reload nginx
```

### Q: 如何查看 Nginx 日志？
```bash
# 错误日志
sudo tail -100 /var/log/nginx/error.log

# 访问日志
sudo tail -100 /var/log/nginx/track_access.log
```

### Q: 修复后还是不行？
1. 检查后端服务是否运行：
```bash
sudo systemctl status track.service
curl http://localhost:8080/actuator/health
```

2. 检查防火墙：
```bash
sudo ufw status
```

3. 查看后端日志：
```bash
sudo journalctl -u track.service -f | grep EventStream
```

---

## 需要帮助？

如果遇到问题，请提供：
1. 执行命令的完整输出
2. `/var/log/nginx/error.log` 的最后100行
3. Nginx 配置文件：`cat /etc/nginx/sites-available/track`

💡 **提示**：最快的方法就是复制"步骤2"的整个命令块，一次性执行！

