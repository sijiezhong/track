# SSE 事件流问题诊断快速指南

## 📋 已完成的改进

### 1. 增强日志输出（纯字符串格式）
✅ 所有控制台日志现在都是纯字符串格式，方便复制
✅ 日志包含完整的调试信息（URL、headers、状态码等）
✅ 使用表情符号区分不同类型的日志

### 2. 创建专用诊断工具
✅ 新增 `/sse-diagnostic` 诊断页面
✅ 提供一键测试功能
✅ 支持复制诊断日志

## 🚀 开始诊断

### 方法 1: 在主页面查看日志（推荐先尝试）

1. 打开浏览器开发者工具 (F12)
2. 切换到 Console (控制台) 标签
3. 访问主页或 BigScreen 页面
4. 查找 `[SSE]` 开头的日志
5. 复制所有相关日志提供给开发者

**正常日志示例：**
```
[SSE] 🔄 准备连接事件流 | URL: /api/v1/events/stream | tenantId: 1 | hasToken: true
[SSE] 📡 正在发起 SSE 连接... | headers: {"Accept":"text/event-stream","Cache-Control":"no-cache","Authorization":"Bearer role:ADMIN","X-Tenant-Id":"1"}
[SSE] 📨 收到响应 | status: 200 | contentType: text/event-stream
[SSE] ✅ 连接成功！正在等待事件数据...
[SSE] 📥 收到新事件 | eventType: click | eventName: button_click | data: {...}
```

### 方法 2: 使用专用诊断页面（问题排查）

1. 确保已登录系统
2. 访问 `http://localhost:5173/sse-diagnostic`
3. 按顺序点击：
   - **1. 测试认证信息** - 确认 token 和 tenantId
   - **2. 测试 SSE 连接** - 建立连接并等待事件
   - **3. 发送测试事件** - 如果连接成功但没有事件
4. 点击 **📋 复制日志** 按钮
5. 将日志粘贴到文本编辑器或发送给开发者

## ⚡ 常见问题快速检查

### 问题 A: 提示 "tenantId 为空"
```
[SSE] ❌ 致命错误: tenantId 为空！请检查登录状态
```
**解决方案：**
- 退出登录后重新登录
- 打开控制台运行：`localStorage.getItem('auth')`
- 如果返回 `null`，说明需要重新登录

### 问题 B: HTTP 401 错误
```
[SSE] ❌ HTTP 错误 | status: 401 | statusText: Unauthorized
```
**解决方案：**
- Token 已过期或无效
- 退出登录后重新登录
- 检查后端是否运行

### 问题 C: 连接成功但一直等待
```
[SSE] ✅ 连接成功！正在等待事件数据...
```
（之后没有更多日志）

**原因：** 连接正常，但系统中没有新事件产生

**解决方案：**
1. 使用诊断页面的 "发送测试事件" 功能
2. 或在其他页面进行操作（点击、页面浏览等）
3. 或运行以下命令发送测试事件：

```bash
# 替换 YOUR_TOKEN 和 YOUR_TENANT_ID
curl -X POST https://zhongsijie.cn/api/v1/events/collect \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-Id: YOUR_TENANT_ID" \
  -d '{
    "eventName": "test_event",
    "eventType": "click",
    "sessionId": "test-session",
    "properties": {
      "test": true
    }
  }'
```

### 问题 D: 网络错误
```
[SSE] ❌ fetch 请求失败 | error: TypeError: Failed to fetch
```
**解决方案：**
- 检查网络连接
- 检查后端服务是否运行：`curl https://zhongsijie.cn/actuator/health`
- 如果是开发环境，检查 Vite 是否正在运行

## 📞 获取帮助

如果以上方法都无法解决问题，请提供以下信息：

1. **浏览器控制台的完整日志**（所有 `[SSE]` 开头的）
2. **诊断页面的输出**（使用"复制日志"功能）
3. **环境信息：**
   - 开发环境还是生产环境？
   - 浏览器类型和版本
   - 是否使用了代理或 VPN？

## 📚 相关文档

- 详细诊断步骤：`SSE_TROUBLESHOOTING.md`
- 代码位置：
  - SSE Hook: `src/hooks/useEventStream.ts`
  - 诊断页面: `src/pages/SSEDiagnostic.tsx`
  - 后端接口: `server/src/main/java/.../EventStreamController.java`

