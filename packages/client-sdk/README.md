# @track/sdk

企业级开源埋点 SDK - 客户端库

## 安装

```bash
npm install @track/sdk
```

## 快速开始

### NPM 包方式

```typescript
import track from '@track/sdk';

// 初始化 SDK（异步）
await track.init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: { plan: 'premium' }
}, {
  endpoint: 'https://track.yourdomain.com',
  sessionTTL: 1440, // 24 小时，0 表示不过期
  autoTrack: true,
  performance: true,
  errorTrack: true
});

// 开始上报
track.start();

// 上报自定义事件
track.track('purchase_success', { 
  productId: '123', 
  price: 99.9 
});

// 停止上报（异步，会销毁 Session）
await track.stop();
```

### Script 标签方式

```html
<script src="https://cdn.yourdomain.com/track-sdk.js"></script>
<script>
  (async function() {
    await window.Track.init({
      appId: 'your-app-id',
      userId: 'user-123'
    }, {
      endpoint: 'https://track.yourdomain.com'
    });
    window.Track.start();
  })();
</script>
```

### Vite Plugin 方式

```typescript
// vite.config.js
import { trackPlugin } from '@track/sdk/vite-plugin'

export default {
  plugins: [
    trackPlugin({
      endpoint: 'https://track.yourdomain.com',
      autoTrack: true,
      performance: true,
      errorTrack: true
    })
  ]
}
```

## API 文档

### init(userConfig, trackConfig?)

初始化 SDK 并注册 Session。**必须先调用此方法，才能调用 start()**。

**参数：**
- `userConfig`: 用户配置
  - `appId`: 应用 ID（必填）
  - `userId`: 用户 ID（必填）
  - `userProps`: 用户属性（可选）
- `trackConfig`: 追踪配置（可选）
  - `endpoint`: 服务端端点地址（必填）
  - `sessionTTL`: Session 存活时长（分钟），默认 1440（24小时），0 表示不过期
  - `autoTrack`: 是否启用自动采集，默认 true
  - `performance`: 是否启用性能监控，默认 false
  - `errorTrack`: 是否启用错误监控，默认 false
  - `batchSize`: 批量上报大小，默认 10
  - `batchWait`: 批量上报等待时间（毫秒），默认 5000
  - `debug`: 是否启用调试模式，默认 false
  - `clickTrack`: 点击采集配置（可选）

**返回：** `Promise<void>`

### start()

开始自动采集数据。**必须在 init() 之后调用**。

### stop()

停止采集并销毁 Session。

**返回：** `Promise<void>`

### track(eventId, properties?)

上报自定义事件。

**参数：**
- `eventId`: 自定义事件唯一标识符（必填）
- `properties`: 事件属性（可选）

## 事件类型

SDK 支持以下事件类型：

- `PAGE_VIEW (1)`: 页面浏览
- `CLICK (2)`: 点击事件
- `PERFORMANCE (3)`: 性能指标
- `ERROR (4)`: 错误监控
- `CUSTOM (5)`: 自定义事件
- `PAGE_STAY (6)`: 页面停留

## 自动采集功能

### PV/UV 统计

SDK 会自动采集页面浏览事件（PAGE_VIEW），用于统计页面访问量和独立访客数。支持：
- 页面首次加载
- SPA 路由变化（pushState/replaceState/hashchange/popstate）

### 点击采集

SDK 提供智能点击采集策略，支持多维度过滤：
- 元素过滤（标签、选择器、属性）
- 内容过滤（文本、可访问性）
- 可见性过滤（隐藏、视口外、尺寸）
- 行为过滤（间隔、频率、连续点击）
- 上下文过滤（首屏、页眉页脚）
- 采样策略（随机、一致性）

详细配置请参考 [点击采集策略文档](../../docs/click-tracking-strategy.md)。

### 性能监控

启用 `performance: true` 后，SDK 会自动采集页面性能指标：
- DNS 查询时间
- TCP 连接时间
- SSL 握手时间
- TTFB (Time To First Byte)
- DOM 解析时间
- 页面加载总时间
- First Paint / First Contentful Paint

### 错误监控

启用 `errorTrack: true` 后，SDK 会自动捕获：
- JavaScript 错误
- 未处理的 Promise 错误

### 页面停留时长

SDK 会自动采集页面停留时长，在页面离开或切换到后台时上报。

## 预设配置

SDK 提供了三种预设的点击采集配置：

```typescript
import { PRESET_CONFIGS } from '@track/sdk';

// 严格模式（高质量数据）
track.init(userConfig, {
  endpoint: '...',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_STRICT
});

// 平衡模式（推荐）
track.init(userConfig, {
  endpoint: '...',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_BALANCED
});

// 宽松模式（全量采集）
track.init(userConfig, {
  endpoint: '...',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_LOOSE
});
```

## 示例项目

查看 `examples/` 目录下的示例项目：
- `vanilla-js/`: 原生 JavaScript 示例
- `vue3/`: Vue 3 示例
- `nextjs/`: Next.js 示例

## 文档

详细文档请参考项目根目录的 README.md。

## License

MIT
