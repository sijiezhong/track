# @track/sdk 客户端使用的服务端接口规范

本文档面向后端实现者，汇总 `@track/sdk` 在浏览器端调用的全部服务端接口，包括：接口地址、HTTP 方法、请求参数、响应语义、状态码、CORS/Cookie 要求与示例请求，便于后续实现与联调。

参考源码（只读）：
- `packages/client-sdk/src/core/tracker.ts`（会话相关：`/api/session`、`/api/session/refresh`、`/api/session/destroy`）
- `packages/client-sdk/src/core/sender.ts`（事件采集：`/api/ingest`，含 sendBeacon/fetch、401/403 处理）
- `packages/client-sdk/src/core/batch.ts`（批量与页面卸载时发送策略）

---

## 约定与通用要求

- 所有接口均为 `POST`，路径前缀以 SDK 配置的 `endpoint` 为基准。
- 浏览器端默认携带 Cookie：`credentials: include`。
- CORS 要求：
  - 响应头需要设置 `Access-Control-Allow-Origin: <前端来源>`（不能为 `*`）。
  - 响应头需要设置 `Access-Control-Allow-Credentials: true`。
  - 正确处理 `OPTIONS` 预检请求（返回允许的方法、头部等）。
- 会话推荐使用 HttpOnly Cookie（建议 `Secure` + `SameSite=Lax`）。
- 响应体建议为 JSON（即使为空，也返回 `{ "ok": true }` 之类统一格式），并配合语义化状态码。

---

## 接口清单

### 1) 初始化会话
- 路径：`POST /api/session`
- 用途：创建/注册一个浏览器会话，并通过 `Set-Cookie` 下发会话标识（SessionId 等）。
- 请求头：`Content-Type: application/json`
- 请求体 JSON：
  - `appId: string` 应用 ID
  - `appName?: string` 项目名（可选；未提供时服务端使用 `appId` 作为项目名）
  - `userId: string` 用户 ID（可为匿名/临时 ID）
  - `userProps?: object` 用户属性（可选，任意键值对）
  - `ttlMinutes?: number | null` 会话有效期（分钟）。`0` 在 SDK 中会被转换为 `null`（表示不过期），未提供时默认 1440（24h）。
- 成功响应（建议）：
  - 状态码：`200`
  - 头部：`Set-Cookie: <会话 Cookie>; HttpOnly; Secure; SameSite=Lax`（根据环境与安全策略调整）
  - 体：`{ "ok": true }` 或包含会话信息的 JSON
- 失败响应：
  - `4xx`：参数错误、鉴权失败等
  - `5xx`：服务端异常

> 说明：若 `appId` 不存在，服务端会自动创建项目（`is_active=true`），`appName` 为空时将使用 `appId` 作为项目名；若项目存在但未激活，将返回 `400` 并附带 `{ code: "INACTIVE_PROJECT", message: "..." }`。

示例（cURL）：
```bash
curl -i -X POST "https://your-endpoint.com/api/session" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "example-app",
    "appName": "Example Project",
    "userId": "user-123",
    "userProps": { "plan": "premium" },
    "ttlMinutes": 1440
  }'
```

---

### 2) 刷新会话
- 路径：`POST /api/session/refresh`
- 用途：刷新会话有效期（基于 Cookie 获取会话标识）。SDK 在上报前会尝试调用，用于延长会话。
- 请求头：`Content-Type: application/json`
- 请求体：无（基于 Cookie 识别会话）
- 成功响应（建议）：
  - 状态码：`200`
  - 体：`{ "ok": true }`
- 失败响应：
  - `401` / `403`：会话不存在或无效（SDK 将尝试重新初始化 `POST /api/session`）
  - 其他 `4xx/5xx`：根据具体错误返回

示例（cURL）：
```bash
curl -i -X POST "https://your-endpoint.com/api/session/refresh" \
  -H "Content-Type: application/json" \
  --cookie "SessionId=<value>"
```

---

### 3) 销毁会话
- 路径：`POST /api/session/destroy`
- 用途：显式终止会话并清除会话 Cookie。
- 请求头：`Content-Type: application/json`
- 请求体：无（基于 Cookie 识别会话）
- 成功响应（建议）：
  - 状态码：`200`
  - 体：`{ "ok": true }`
  - 头部：清除会话 Cookie（如 `Set-Cookie: SessionId=; Max-Age=0`）
- 失败响应：
  - `401` / `403`：会话无效或未登录
  - 其他 `4xx/5xx`：根据具体错误返回

示例（cURL）：
```bash
curl -i -X POST "https://your-endpoint.com/api/session/destroy" \
  -H "Content-Type: application/json" \
  --cookie "SessionId=<value>"
```

---

### 4) 事件采集（批量）
- 路径：`POST /api/ingest`
- 用途：接收 SDK 发送的事件数据（批量）。SDK 首选 `navigator.sendBeacon` 发送；失败时使用 `fetch` 发送并携带 Cookie。
- 请求头：`Content-Type: application/json`
- 请求体 JSON（字段经过压缩）：
  - 顶层：`{ e: Array<EventItem> }`
  - `EventItem`：`{ t: number, id?: string, p?: object }`
    - `t`：事件类型（与 SDK `EventType` 枚举对应，数值枚举）
    - `id`：自定义事件 ID（仅自定义事件存在）
    - `p`：事件属性对象（SDK 侧已去除 `null/undefined`）
- 成功响应（建议）：
  - 状态码：`200`
  - 体：`{ "ok": true }`
- 失败响应：
  - `401` / `403`：会话失效（SDK 会触发重新初始化，会在后续重试）
  - `413`：请求体过大（可提示客户端调整批次大小）
  - 其他 `4xx/5xx`：根据具体错误返回
- 兼容性：SDK 在 `fetch` 模式下使用 `keepalive: true`，以便在页面卸载时也能发送；`sendBeacon` 路径无需返回体，但建议保持 `200`。

示例（cURL）：
```bash
curl -i -X POST "https://your-endpoint.com/api/ingest" \
  -H "Content-Type: application/json" \
  --cookie "SessionId=<value>" \
  -d '{
    "e": [
      { "t": 9, "id": "button_click", "p": { "buttonId": "test-btn", "category": "action" } },
      { "t": 1, "p": { "path": "/", "title": "Home" } }
    ]
  }'
```
> 说明：上述示例中的 `t` 数值仅为示意，具体取值以 SDK 的 `EventType` 实际映射为准。

---

## 状态码与错误语义建议
- `200`：成功
- `400`：请求参数错误（缺少字段、格式不合法等）
- `401`：未认证/会话无效（Cookie 不存在或无效）
- `403`：已认证但无权限/会话过期
- `413`：请求体过大
- `429`：请求过于频繁（可选，服务端限流）
- `5xx`：服务端错误

---

## 安全与合规建议
- 会话 Cookie 建议使用 `HttpOnly`、`Secure`、`SameSite=Lax`（生产环境 HTTPS）。
- 对 `/api/ingest` 做基础限流与数据校验，防御恶意/异常上报。
- 对 `userProps`、事件属性 `p` 做字段白名单或体量限制，避免超大 JSON 或敏感数据入库。
- 按需打通多租户与 `appId` 绑定的鉴权校验。

---

## 调试与联调建议
- 在浏览器 Network 面板观察 `POST /api/session`、`/api/ingest` 等请求与响应头，确认：
  - CORS 与 `Access-Control-Allow-Credentials` 设置正确；
  - 会话 Cookie 正常下发与携带；
  - 401/403 时客户端是否重新初始化并恢复；
- 使用 `curl` 复现接口调用，排查网络、CORS、鉴权与 Cookie 问题。

---

## 附：字段速查
- `appId`：应用 ID（字符串）
- `userId`：用户 ID（字符串）
- `userProps`：用户属性对象（可选）
- `ttlMinutes`：会话有效期分钟数（`0` 在 SDK 中会转换为 `null` 表示不过期；未提供默认 1440）
- `t`：事件类型（数值，对应 SDK `EventType`）
- `id`：自定义事件 ID（仅自定义事件包含）
- `p`：事件属性（对象，已去除空值）

---

如需扩展：
- 可在 `/api/ingest` 返回体中附带采集策略、抽样参数等（前端可忽略不可识别字段）。
- 支持 gzip/deflate 压缩（建议按需开启，前后端协商）。

