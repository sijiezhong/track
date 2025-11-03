# SSE 事件流问题诊断指南

## 问题描述
实时事件流一直显示"等待事件数据..."

## 诊断步骤

### 1. 检查浏览器控制台日志

打开浏览器开发者工具 (F12)，查看控制台 (Console) 输出，查找以下日志：

#### ✅ 正常情况应该看到：
```
[SSE] 🔄 准备连接事件流 { url: '/api/v1/events/stream', tenantId: 1, hasToken: true }
[SSE] 📡 正在发起 SSE 连接...
[SSE] 📨 收到响应 { status: 200, contentType: 'text/event-stream' }
[SSE] ✅ 连接成功！正在等待事件数据...
```

#### ❌ 常见错误情况：

**情况 1: 认证问题**
```
[SSE] ❌ 致命错误: tenantId 为空！请检查登录状态
[SSE] ❌ 致命错误: token 为空！请检查登录状态
```
**解决方案**: 
- 退出登录后重新登录
- 检查 localStorage 中是否有 `auth` 信息
- 运行：`localStorage.getItem('auth')` 查看认证信息

**情况 2: 网络错误**
```
[SSE] ❌ fetch 请求失败: TypeError: Failed to fetch
```
**解决方案**:
- 检查后端服务是否运行
- 检查代理配置是否正确
- 检查网络连接

**情况 3: HTTP 错误**
```
[SSE] ❌ HTTP 错误: { status: 401, statusText: 'Unauthorized' }
[SSE] ❌ HTTP 错误: { status: 403, statusText: 'Forbidden' }
[SSE] ❌ HTTP 错误: { status: 500, statusText: 'Internal Server Error' }
```
**解决方案**:
- 401: Token 过期或无效，重新登录
- 403: 权限不足，检查用户权限
- 500: 后端错误，查看后端日志

### 2. 检查网络请求

在开发者工具的 Network (网络) 标签中：

1. 过滤 `stream` 关键词
2. 查找 `/api/v1/events/stream` 请求
3. 检查请求状态：
   - **Status: 200** ✅ 连接成功
   - **Status: Pending** ⏳ 正常，SSE 是长连接
   - **Status: 401/403** ❌ 认证/权限问题
   - **Status: 500** ❌ 后端错误

4. 查看 Request Headers：
   - 应该包含 `Authorization: Bearer <token>`
   - 应该包含 `X-Tenant-Id: <tenantId>`
   - 应该包含 `Accept: text/event-stream`

5. 查看 Response Headers：
   - 应该包含 `Content-Type: text/event-stream`
   - 应该包含 `Cache-Control: no-cache`

### 3. 检查后端服务

#### 检查后端是否运行
```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 或者检查生产环境
curl https://zhongsijie.cn/actuator/health
```

#### 测试 SSE 端点（需要替换 token 和 tenantId）
```bash
# 测试 SSE 连接
curl -N \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-Id: YOUR_TENANT_ID" \
  -H "Accept: text/event-stream" \
  https://zhongsijie.cn/api/v1/events/stream
```

应该立即看到：
```
event: init
data: ok
```

### 4. 触发测试事件

SSE 连接成功后，如果没有事件产生，就不会收到数据。需要主动触发一些事件：

**方法 1: 使用 SDK 示例项目**
```bash
cd sdk/examples/vue3-example
pnpm install
pnpm dev
# 访问页面并点击按钮，触发事件
```

**方法 2: 直接发送事件 API**
```bash
curl -X POST https://zhongsijie.cn/api/v1/events/collect \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: YOUR_TENANT_ID" \
  -d '{
    "eventName": "test_event",
    "sessionId": "test-session",
    "tenantId": YOUR_TENANT_ID,
    "properties": {
      "source": "manual_test"
    }
  }'
```

**方法 3: 使用浏览器控制台**
在已登录的页面打开控制台，运行：
```javascript
fetch('/api/v1/events/collect', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${JSON.parse(localStorage.getItem('auth')).token}`,
    'X-Tenant-Id': JSON.parse(localStorage.getItem('auth')).tenantId
  },
  body: JSON.stringify({
    eventName: 'manual_test',
    eventType: 'click',
    sessionId: 'test-' + Date.now(),
    properties: {
      test: true
    }
  })
})
```

### 5. 常见问题排查

#### 问题 A: 连接成功但没有事件
**原因**: 系统中没有产生新的事件
**解决**: 
- 使用上面的方法触发测试事件
- 让其他用户或测试工具产生事件
- 检查后端 EventService.createEvent() 是否被调用
- 检查后端 broadcaster.broadcastEvent() 是否被执行

#### 问题 B: CORS 跨域错误
**原因**: 后端 CORS 配置问题
**解决**: 检查后端 CORS 配置，确保允许前端域名

#### 问题 C: 代理超时
**原因**: Vite 代理配置的超时时间太短
**解决**: 已在 vite.config.ts 中设置 `timeout: 0`

#### 问题 D: 连接频繁断开
**原因**: 
- 网络不稳定
- 后端 SSE 超时时间太短（默认 30 分钟）
- Nginx 或其他代理的超时配置

**解决**: 
- 检查网络连接
- 调整后端 EventStreamBroadcaster 的 DEFAULT_TIMEOUT_MS
- 配置 Nginx proxy_read_timeout

## 快速检查清单

- [ ] 用户已登录，localStorage 中有 auth 信息
- [ ] 后端服务正常运行
- [ ] Network 中看到 /api/v1/events/stream 请求
- [ ] 请求状态为 200 或 Pending
- [ ] 控制台显示 "✅ 连接成功！正在等待事件数据..."
- [ ] 有实际的事件产生（通过测试或真实用户行为）

## 获取更多帮助

如果以上步骤都无法解决问题，请提供：

1. 浏览器控制台完整日志（包含 [SSE] 前缀的）
2. Network 标签中 stream 请求的详细信息
3. 后端服务日志（查找 EventStreamController 和 EventStreamBroadcaster 相关的）
4. 当前使用的环境（开发/生产）

