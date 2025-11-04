# @dashboard 前端使用的服务端接口规范

本文档面向后端实现者，汇总仪表盘（Dashboard）前端调用的全部服务端接口，包括：接口地址、HTTP 方法、请求参数、响应语义、状态码与示例请求，便于后续实现与联调。

参考实现（只读）：
- `packages/dashboard/src/services/http.ts`（axios 实例与错误提示拦截器）
- `packages/dashboard/src/services/analytics.ts`（分析相关接口）
- `packages/dashboard/src/services/events.ts`（事件列表）
- `packages/dashboard/src/services/projects.ts`（应用列表）

---

## 约定与通用要求

- 所有接口为 `GET`（与当前前端实现一致）。
- 基础地址：由环境变量 `VITE_API_BASE_URL` 配置，前端通过 axios 的 `baseURL` 拼接请求。
- 超时：15s。
- 错误处理：
  - 建议返回体包含 `message` 与可选 `traceId` 字段，前端会用作用户提示。
  - `429`（频率受限）可携带响应头 `Retry-After`（秒），前端会在提示语中呈现。
- CORS/Cookie：本仪表盘为管理端，默认不强制携带 Cookie；是否需要鉴权、跨域、Cookie 由后端网关/鉴权层自行定义。

---

## 接口清单

### 1) 概览 KPI
- 路径：`GET /api/analytics/overview`
- 用途：获取指定 `appId`、时间区间内的 PV、UV、跳出率与平均停留。
- 查询参数：
  - `appId?: string` 应用 ID（可选，缺省表示全量）
  - `start?: string` 开始时间（ISO 字符串，前端以 `YYYY-MM-DDTHH:mm` 传输；展示仅显示到日）
  - `end?: string` 结束时间（同上）
- 成功响应：
  - `200`，`{ pv: number; uv: number; bounceRate: number; avgDurationSec: number; timezone: string }`
- 失败响应：`4xx/5xx`，建议包含 `message` 与可选 `traceId`
- 示例：
```bash
curl -G "$BASE/api/analytics/overview" \
  --data-urlencode "appId=example-app" \
  --data-urlencode "start=2025-01-01T00:00" \
  --data-urlencode "end=2025-01-07T23:59"
```

---

### 2) PV/UV 趋势
- 路径：`GET /api/analytics/pv-uv/series`
- 用途：获取区间内按时间粒度聚合的 PV/UV 序列。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `interval: 'minute' | 'hour' | 'day'`
- 成功响应：
  - `200`，`{ series: { ts: string; pv: number; uv: number }[]; interval: string; timezone: string }`
- 失败响应：同上
- 示例：
```bash
curl -G "$BASE/api/analytics/pv-uv/series" \
  --data-urlencode "appId=example-app" \
  --data-urlencode "start=2025-01-01T00:00" \
  --data-urlencode "end=2025-01-07T23:59" \
  --data-urlencode "interval=hour"
```

---

### 3) 页面 Top 列表
- 路径：`GET /api/analytics/pages/top`
- 用途：获取区间内页面指标 TopN。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `limit?: number`（默认 10）
- 成功响应：
  - `200`，`{ list: { pageUrl: string; pv: number; uv: number; avgDurationSec: number }[]; total: number }`
- 示例：
```bash
curl -G "$BASE/api/analytics/pages/top" \
  --data-urlencode "appId=example-app" \
  --data-urlencode "start=2025-01-01T00:00" \
  --data-urlencode "end=2025-01-07T23:59" \
  --data-urlencode "limit=10"
```

---

### 4) 事件类型分布
- 路径：`GET /api/analytics/events-distribution`
- 用途：区间内事件类型占比分布。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
- 成功响应：
  - `200`，`{ list: { type: string; value: number }[] }`

---

### 5) Web Vitals 分位
- 路径：`GET /api/analytics/web-vitals`
- 用途：区间内 Web Vitals 指标（单指标）的分位统计。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `metric?: string`（如 `LCP`）
- 成功响应：
  - `200`，`{ p50: number; p75: number; p95: number; unit: string }`

---

### 6) 自定义事件趋势
- 路径：`GET /api/analytics/custom-events`
- 用途：按事件 ID 聚合的趋势数据。
- 查询参数：
  - `appId?: string`
  - `eventId?: string`
  - `start: string`
  - `end: string`
  - `groupBy?: 'hour' | 'day'`
- 成功响应：
  - `200`，`{ series: { ts: string; count: number }[]; total: number; groupBy: string }`

---

### 7) 自定义事件 Top 列表
- 路径：`GET /api/analytics/custom-events/top`
- 用途：区间内自定义事件 TopN。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `limit?: number`
- 成功响应：
  - `200`，`{ list: { eventId: string; count: number }[]; total: number }`

---

### 8) Web Vitals 趋势
- 路径：`GET /api/analytics/web-vitals/series`
- 用途：区间内 Web Vitals 指标趋势。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `metric: string`
  - `interval?: 'hour' | 'day'`
- 成功响应：
  - `200`，`{ series: { ts: string; p50: number; p75: number; p95: number }[]; interval: string; timezone: string }`

---

### 9) 错误趋势
- 路径：`GET /api/analytics/errors/trend`
- 用途：区间内错误数量趋势。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `interval?: 'hour' | 'day'`
- 成功响应：
  - `200`，`{ series: { ts: string; count: number }[]; interval: string; timezone: string }`

---

### 10) 错误 Top 列表
- 路径：`GET /api/analytics/errors/top`
- 用途：区间内错误 TopN。
- 查询参数：
  - `appId?: string`
  - `start: string`
  - `end: string`
  - `limit?: number`
- 成功响应：
  - `200`，`{
      list: {
        fingerprint: string;
        message: string;
        count: number;
        firstSeen: string;
        lastSeen: string;
      }[];
      total: number;
    }`

---

### 11) 事件列表
- 路径：`GET /api/events`
- 用途：查询事件记录（用于“用户行为路径”等功能）。
- 查询参数：
  - `appId?: string`
  - `start?: string`
  - `end?: string`
  - `type?: number`
  - `keyword?: string`
  - `page?: number`
  - `size?: number`
- 成功响应：
  - `200`，`{ items: Record<string, any>[]; page: { index: number; size: number; total: number } }`

---

### 12) 应用列表
- 路径：`GET /api/projects`
- 用途：获取可选的应用列表供筛选。
- 查询参数：
  - `active?: boolean` 是否只返回启用中的应用
- 成功响应：
  - `200`，`{ list: { appId: string; appName: string }[] }`

---

## 状态码与错误语义建议
- `200`：成功
- `400`：请求参数错误（缺少字段、格式不合法等）
- `401/403`：未认证或无权限（如需鉴权）
- `429`：请求过于频繁（可选，配合 `Retry-After`）
- `5xx`：服务端错误

---

## 备注
- 前端展示时间仅显示到“日”，但查询参数依然携带到分钟（`YYYY-MM-DDTHH:mm`）。


