# Track SDK

前端埋点 SDK，支持自动采集和手动上报，兼容多框架，提供纯 JS 引入和 Vite Plugin 两种使用方式。

## 特性

- 🚀 **自动采集**：页面访问、点击事件、性能数据、错误监控
- 📦 **批量上报**：本地队列管理，批量阈值触发或超时自动上报
- 🔄 **重试机制**：网络异常自动重试，支持指数退避
- 👤 **用户管理**：支持实名/匿名模式切换，用户身份追踪
- 🔌 **多框架支持**：框架无关，可用于 React、Vue、Angular 等
- 🎯 **多实例支持**：支持同一页面多个项目实例
- 📱 **浏览器兼容**：支持 IE11+ 和所有现代浏览器
- 🛠️ **TypeScript**：完整的 TypeScript 类型定义

## 安装

```bash
npm install @track/sdk
```

## 快速开始

### 方式一：纯 JS 引入

```typescript
import { init } from '@track/sdk';

// 初始化 SDK
const tracker = init({
  endpoint: 'https://api.example.com',
  projectId: 1, // 对应服务端的 tenantId
  autoStart: true, // 自动启动采集
  debug: true, // 开发模式
});

// 手动上报事件
tracker.trackEvent('custom_event', {
  key: 'value',
  timestamp: Date.now(),
});

// 设置用户身份
tracker.setUser({
  userId: 123,
  userName: 'John Doe',
});

// 切换到匿名模式
tracker.clearUser();
```

### 方式二：Vite Plugin

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import { trackPlugin } from '@track/sdk/vite-plugin';

export default defineConfig({
  plugins: [
    trackPlugin({
      endpoint: 'https://api.example.com',
      projectId: 1,
      autoStart: true,
      collectors: ['pageview', 'click', 'error'],
      debug: process.env.NODE_ENV === 'development',
    }),
  ],
});
```

在应用代码中使用：

```typescript
// 获取自动注入的 tracker 实例
const tracker = window.__trackSDK;

// 或者从全局对象获取（如果已注入）
tracker.trackEvent('custom_event', { key: 'value' });
```

### 方式三：UMD 格式（Script 标签）

```html
<script src="https://cdn.example.com/track-sdk/index.umd.js"></script>
<script>
  // 初始化
  const tracker = TrackSDK.init({
    endpoint: 'https://api.example.com',
    projectId: 1,
    autoStart: true,
  });

  // 使用
  tracker.trackEvent('pageview', { url: window.location.href });
</script>
```

## 配置选项

### TrackerConfig

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| endpoint | string | 是 | - | 服务端 API 地址 |
| projectId | string \| number | 是 | - | 项目 ID（对应服务端的 tenantId） |
| autoStart | boolean | 否 | true | 是否自动启动采集 |
| batchSize | number | 否 | 10 | 批量上报阈值 |
| batchTimeout | number | 否 | 5000 | 批量上报超时时间（毫秒） |
| retry | RetryConfig | 否 | - | 重试配置 |
| collectors | CollectorConfig | 否 | - | 采集器配置 |
| debug | boolean | 否 | false | 是否启用调试模式 |
| usePixel | boolean | 否 | false | 是否使用像素上报（1x1 GIF） |

### RetryConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| maxRetries | number | 5 | 最大重试次数 |
| retryDelay | number | 1000 | 初始重试延迟（毫秒） |
| retryBackoff | number | 2 | 重试退避倍数（指数退避） |

### CollectorConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageview | boolean | true | 是否启用页面访问采集 |
| click | boolean | true | 是否启用点击事件采集 |
| performance | boolean | false | 是否启用性能数据采集 |
| error | boolean | true | 是否启用错误采集 |

## API 文档

### 初始化

#### `init(config: TrackerConfig): Tracker`

创建并初始化 Tracker 实例。

#### `createTracker(config: TrackerConfig): Tracker`

创建 Tracker 实例（不自动启动）。

#### `getTracker(projectId: string | number): Tracker | undefined`

获取指定项目 ID 的 Tracker 实例。

#### `destroyTracker(projectId: string | number): void`

销毁 Tracker 实例。

### Tracker 实例方法

#### `start(): void`

启动自动采集。

#### `stop(): void`

停止自动采集。

#### `setUser(userInfo: UserInfo): void`

设置用户身份（切换到实名模式）。

```typescript
tracker.setUser({
  userId: 123,
  userName: 'John Doe',
  email: 'john@example.com',
});
```

#### `clearUser(): void`

清除用户身份（切换到匿名模式）。

#### `trackEvent(eventType: string, eventContent?: Record<string, unknown>): void`

手动上报事件。

```typescript
tracker.trackEvent('purchase', {
  productId: '12345',
  price: 99.99,
  currency: 'USD',
});
```

#### `trackPageView(url?: string, title?: string): void`

手动上报页面访问事件。

#### `flush(): void`

立即上报队列中的所有事件。

#### `getSessionId(): string`

获取当前会话 ID。

#### `getAnonymousId(): string`

获取匿名 ID。

#### `getUserId(): string | number | null`

获取用户 ID（如果已设置）。

#### `getUserMode(): UserMode`

获取当前用户模式（实名/匿名）。

## 事件类型

### 自动采集事件

- **pageview**: 页面访问事件
- **click**: 点击事件
- **performance**: 性能数据
- **error**: 错误事件

### 自定义事件

使用 `trackEvent()` 方法上报自定义事件类型。

## 数据格式

### 事件数据结构

```typescript
interface EventData {
  event_type: string; // 事件类型
  event_content?: Record<string, unknown>; // 事件内容
}
```

### 上报到服务端的数据

SDK 只上报 `event_type` 和 `event_content`，其他字段（如 `timestamp`、`user_id`、`session_id`、`project_id` 等）由服务端自动补全。

## 批量上报策略

- **批量阈值触发**：队列达到 `batchSize` 时立即上报
- **超时触发**：达到 `batchTimeout` 时自动上报
- **页面卸载兜底**：`beforeunload`` 事件时上报未上报的事件

## 错误处理

SDK 内置重试机制：

- 默认最大重试 5 次
- 指数退避策略（每次重试延迟翻倍）
- 失败事件会重新加入队列

## 浏览器兼容性

- Chrome/Edge (最新版本)
- Firefox (最新版本)
- Safari (最新版本)
- IE11+（需要 polyfill）

## 开发

```bash
# 安装依赖
npm install

# 开发模式（监听文件变化）
npm run dev

# 构建
npm run build

# 类型检查
npm run type-check

# 测试
npm test

# 测试覆盖率
npm run test:coverage
```

## 许可证

MIT

## 相关链接

- [后端 API 文档](../API_DOCUMENTATION.md)
- [业务需求](../BUSINESS_REQUIREMENTS.md)
- [开发文档](../DEVELOPMENT.md)

