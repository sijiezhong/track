# 🔧 修复 SSE 连接问题 - Nginx 配置

## 问题诊断结果

✅ **问题已找到：Nginx 配置路径不匹配**

### 问题详情

1. **后端 API 实际路径**：`/api/v1/events/stream`
2. **Nginx 配置的路径**：`/api/events/stream`  ❌ 不匹配
3. **结果**：SSE 请求走了普通代理配置，缺少 SSE 必需的配置（禁用buffering、长超时等），导致连接阻塞超时

### 测试证据

```bash
# 直接请求后端超时（5秒无响应）
curl --max-time 5 -H "Authorization: Bearer role:ADMIN" \
     -H "X-Tenant-Id: 1" \
     https://zhongsijie.cn/api/v1/events/stream
# 结果：Operation timed out after 5003 milliseconds with 0 bytes received
```

## 🚀 修复步骤

### 1. 更新 Nginx 配置

SSH 登录到服务器，编辑 Nginx 配置文件：

```bash
# 登录服务器
ssh user@zhongsijie.cn

# 备份当前配置
sudo cp /etc/nginx/sites-available/track /etc/nginx/sites-available/track.backup

# 编辑配置文件
sudo nano /etc/nginx/sites-available/track
```

### 2. 添加/修改 SSE 配置块

在配置文件中找到或添加以下内容（在 `server` 块内，在通用的 `location /` 之前）：

```nginx
# ===== 重要：SSE 端点特殊配置 =====
location /api/v1/events/stream {
    proxy_pass http://localhost:8080/api/v1/events/stream;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 转发认证头（关键！）
    proxy_set_header Authorization $http_authorization;
    proxy_set_header X-Tenant-Id $http_x_tenant_id;
    
    # SSE 特定配置
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
    proxy_buffering off;  # 必须禁用 buffering
    proxy_cache off;      # 必须禁用缓存
    
    # 超时设置（SSE 需要长连接）
    proxy_connect_timeout 60s;
    proxy_send_timeout 3600s;  # 1小时
    proxy_read_timeout 3600s;  # 1小时
}
```

### 3. 测试并重启 Nginx

```bash
# 测试配置文件语法
sudo nginx -t

# 如果测试通过，重启 Nginx
sudo systemctl reload nginx

# 查看 Nginx 状态
sudo systemctl status nginx

# 查看 Nginx 错误日志（如果有问题）
sudo tail -f /var/log/nginx/error.log
```

### 4. 验证修复

修复后，再次测试 SSE 连接：

```bash
# 应该立即收到 "event: init" 消息
curl -N -H "Authorization: Bearer role:ADMIN" \
        -H "X-Tenant-Id: 1" \
        https://zhongsijie.cn/api/v1/events/stream

# 期望输出：
# event: init
# data: ok
#
# （然后连接保持打开状态）
```

### 5. 前端测试

1. 刷新浏览器页面
2. 打开控制台，应该看到：
```
[SSE] 🔄 准备连接事件流 | URL: /api/v1/events/stream | tenantId: 1 | hasToken: true
[SSE] 📡 正在发起 SSE 连接...
[SSE] 📨 收到响应 | status: 200 | contentType: text/event-stream
[SSE] ✅ 连接成功！正在等待事件数据...
```

## 📝 完整配置文件参考

完整的修复后配置文件已保存到：`/Users/zhongsijie/code/track/nginx.conf.fixed`

可以直接使用该文件替换服务器上的配置。

## ⚠️ 注意事项

1. **location 顺序很重要**：SSE 的 `location /api/v1/events/stream` 必须在通用的 `location /` 之前
2. **headers 转发**：确保转发 `Authorization` 和 `X-Tenant-Id` 头
3. **禁用 buffering**：`proxy_buffering off;` 是 SSE 工作的关键
4. **长超时**：SSE 是长连接，需要设置长的超时时间

## 🔍 如果修复后仍有问题

1. 查看 Nginx 错误日志：
```bash
sudo tail -100 /var/log/nginx/error.log
```

2. 查看后端 Spring Boot 日志：
```bash
# 应该看到：客户端订阅事件流: tenantId=1
sudo journalctl -u track.service -f | grep EventStream
```

3. 检查后端是否正常运行：
```bash
curl http://localhost:8080/actuator/health
```

## 📞 需要帮助？

如果以上步骤完成后仍无法解决问题，请提供：
- Nginx 配置文件内容
- Nginx 错误日志
- 后端服务日志
- 浏览器控制台日志

