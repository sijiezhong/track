# Track - 企业级开源埋点项目

## 项目概述

Track 是一个轻量级企业级开源埋点数据分析平台，提供从数据采集、存储到分析的全链路解决方案。

### 架构特点

- **单公司多项目设计**：支持一个公司管理多个业务项目（AppId），每个项目独立的数据采集与分析
- **基于 Cookie 的 Session 机制**：通过 Session 减少字段传递，大幅降低事件 payload 大小
- **CORS 跨域上报**：服务端配置 CORS 支持，使用 sendBeacon/fetch POST 方式上报数据

## 系统架构

```
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  客户端 SDK     │───▶│   服务端 API     │───▶│  数据分析平台    │
│                 │    │                  │    │                  │
│ • Web SDK       │    │ • Spring Boot    │    │ • React          │
│ • Vite Plugin   │    │ • PostgreSQL     │    │ • G2/G6          │
│ • TypeScript    │    │ • Redis          │    │ • shadcn-ui      │
└─────────────────┘    └──────────────────┘    └──────────────────┘
                              │
                         ┌────────────┐
                         │ PostgreSQL │
                         └────────────┘
```

## 1. 项目目录结构

```
track/
├── packages/
│   ├── client-sdk/
│   │   ├── src/
│   │   │   ├── core/
│   │   │   │   ├── tracker.ts
│   │   │   │   ├── storage.ts
│   │   │   │   ├── sender.ts
│   │   │   │   └── batch.ts
│   │   │   ├── types/
│   │   │   ├── plugins/
│   │   │   │   ├── auto-track.ts
│   │   │   │   ├── performance.ts
│   │   │   │   └── error.ts
│   │   │   ├── utils/
│   │   │   └── index.ts
│   │   ├── vite-plugin/
│   │   ├── dist/
│   │   ├── package.json
│   │   └── vite.config.ts
│   │
│   ├── server/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/track/
│   │   │   │   │   ├── Application.java
│   │   │   │   │   ├── config/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── aspect/
│   │   │   │   └── resources/
│   │   │   └── test/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── docker-compose.yml
│   │
│   └── dashboard/
│       ├── src/
│       │   ├── components/
│       │   ├── pages/
│       │   ├── hooks/
│       │   ├── utils/
│       │   ├── types/
│       │   ├── services/
│       │   ├── store/
│       │   ├── router/
│       │   └── App.tsx
│       ├── public/
│       ├── package.json
│       ├── vite.config.ts
│       ├── tailwind.config.js
│       └── components.json
│
├── docs/
├── scripts/
├── .github/
├── docker-compose.prod.yml
└── README.md
```

## 2. 客户端 SDK 详细需求

### 2.1 核心功能
- **初始化机制**：必须先调用 `init` 初始化用户信息并注册 Session，才能调用 `start` 开始上报
- **Session 管理**：基于 Cookie 的 Session 机制，自动刷新和失效重试
- **中断上报**：支持 `stop` 方法暂停数据上报并立即销毁 Session
- **自动采集**：
  - **PV/UV 统计**：自动采集页面浏览事件（PAGE_VIEW），用于统计页面访问量和独立访客数
  - **用户点击行为**：智能采集用户点击事件，支持多维度过滤策略，只采集有价值的交互行为（详见 [点击采集策略文档](./docs/click-tracking-strategy.md)）
  - 页面性能指标
  - JavaScript 错误监控
  - 页面停留时长
- **自定义事件**：支持唯一标识符区分不同自定义事件

### 2.2 前后端约定的事件类型枚举

**重要**：以下枚举值是客户端与服务端的约定，双方必须保持一致。使用数字类型可以减少传输长度。

#### 2.2.1 客户端定义（TypeScript）

```typescript
/**
 * 事件类型枚举
 * 注意：此枚举值与服务端必须完全一致，修改时需同步更新服务端
 */
enum EventType {
  PAGE_VIEW = 1,      // 页面浏览
  CLICK = 2,          // 点击事件
  PERFORMANCE = 3,    // 性能指标
  ERROR = 4,          // 错误监控
  CUSTOM = 5,         // 自定义事件
  PAGE_STAY = 6       // 页面停留
}
```

#### 2.2.2 服务端定义（Java）

```java
/**
 * 事件类型枚举
 * 注意：此枚举值与客户端必须完全一致，修改时需同步更新客户端
 */
public enum EventType {
    PAGE_VIEW(1, "page_view", "页面浏览"),
    CLICK(2, "click", "点击事件"),
    PERFORMANCE(3, "performance", "性能指标"),
    ERROR(4, "error", "错误监控"),
    CUSTOM(5, "custom", "自定义事件"),
    PAGE_STAY(6, "page_stay", "页面停留");
    
    private final int code;
    private final String name;
    private final String description;
    
    EventType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static EventType fromCode(int code) {
        for (EventType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type code: " + code);
    }
}
```

#### 2.2.3 数据库定义

```sql
-- 事件类型枚举表（与客户端和服务端枚举值保持一致）
CREATE TABLE event_types (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);

-- 预置事件类型（ID 必须与枚举值一致）
INSERT INTO event_types (id, name, description) VALUES
(1, 'page_view', '页面浏览'),
(2, 'click', '点击事件'),
(3, 'performance', '性能指标'),
(4, 'error', '错误监控'),
(5, 'custom', '自定义事件'),
(6, 'page_stay', '页面停留');
```

**约定说明**：
- 客户端上报时使用数字类型（1-6）
- 服务端接收后转换为对应的枚举值
- 数据库存储时使用 `event_type_id` 字段存储数字值
- 三方必须保持一致，任何修改都需要同步更新

### 2.3 API 设计

#### 2.3.1 单例模式设计

**核心设计**：Track SDK 采用单例模式，确保整个应用中只有一个实例，防止重复初始化导致的配置冲突和资源浪费。

**优势**：
- 防止用户在多个地方创建多个实例
- 确保全局配置一致性
- 避免重复的事件监听和资源占用
- 简化使用方式，无需手动管理实例

**实现方式**：
- 使用 `Track.getInstance()` 获取单例实例
- 直接导出单例实例 `track` 供使用
- 私有构造函数防止外部 `new Track()`

**注意事项**：
- 多次调用 `init()` 会覆盖之前的配置，建议只在应用启动时调用一次
- 如果需要在不同场景使用不同配置，应使用 `stop()` 后再重新 `init()`

#### 2.3.2 Session 机制

**核心设计**：通过基于 Cookie 的 Session 机制减少字段传递，用户信息（`appId`、`userId`、`userProps`）在 `init` 时注册到服务端，后续上报时通过 Cookie 自动传递 `sessionId`，事件数据中不再包含用户信息。

**Session 生命周期**：
- **注册**：`init()` 时调用 `POST /api/session` 注册 session，服务端返回 `sessionId` 并设置到 Cookie
- **刷新**：每次触发采集事件时自动调用 `POST /api/session/refresh` 刷新 Cookie 过期时间
- **失效处理**：当 sessionId 失效（HTTP 401/403）时，自动重新调用 `init()` 注册新 session
- **销毁**：调用 `stop()` 时立即调用 `POST /api/session/destroy` 销毁服务端 session 并清除 Cookie

**Cookie 配置**：
- 名称：`track_session_id`
- 存活时长：默认 24 小时（1440 分钟），通过 `TrackConfig.sessionTTL` 配置（单位：分钟）
- 为 0 时表示不过期（永久有效）
- 属性：`HttpOnly`、`SameSite=Lax`、`Path=/`

```typescript
interface UserConfig {
  appId: string;
  userId: string;
  userProps?: Record<string, any>;
}

interface TrackConfig {
  endpoint: string;
  sessionTTL?: number;  // Session 存活时长（分钟），默认 1440（24小时），0 表示不过期
  autoTrack?: boolean;
  performance?: boolean;
  errorTrack?: boolean;
  batchSize?: number;
  batchWait?: number;
  debug?: boolean;
}

// 事件数据结构（不再包含 user 字段）
interface EventData {
  type: EventType;
  eventId?: string;  // 自定义事件唯一标识符
  properties: Record<string, any>;
}

class Track {
  private static instance: Track | null = null;
  private initialized: boolean = false;
  private started: boolean = false;
  private userConfig: UserConfig | null = null;
  private trackConfig: TrackConfig | null = null;
  
  // 私有构造函数，防止外部实例化
  private constructor() {}
  
  // 获取单例实例
  static getInstance(): Track {
    if (!Track.instance) {
      Track.instance = new Track();
    }
    return Track.instance;
  }
  
  // 初始化用户信息并注册 Session（必须先调用，异步）
  async init(userConfig: UserConfig, trackConfig?: TrackConfig): Promise<void> {
    this.userConfig = userConfig;
    this.trackConfig = trackConfig || {};
    
    // 调用服务端注册 Session
    const sessionTTL = this.trackConfig.sessionTTL ?? 1440; // 默认 24 小时
    const response = await fetch(`${this.trackConfig.endpoint}/api/session`, {
      method: 'POST',
      mode: 'cors',
      credentials: 'include', // 重要：允许携带 Cookie
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        appId: userConfig.appId,
        userId: userConfig.userId,
        userProps: userConfig.userProps || {}
      })
    });
    
    if (!response.ok) {
      throw new Error('Failed to initialize session');
    }
    
    // Cookie 由服务端自动设置，浏览器会自动管理
    this.initialized = true;
    this.storage.saveUserConfig(userConfig);
    this.storage.saveTrackConfig(this.trackConfig);
  }
  
  // 开始上报（必须在 init 后调用）
  start(): void {
    if (!this.initialized) {
      throw new Error('Must call init() before start()');
    }
    this.started = true;
    this.setupAutoTrack();
  }
  
  // 停止上报并销毁 Session
  async stop(): Promise<void> {
    this.started = false;
    this.removeAutoTrack();
    
    // 销毁服务端 Session 并清除 Cookie
    if (this.trackConfig) {
      try {
        await fetch(`${this.trackConfig.endpoint}/api/session/destroy`, {
          method: 'POST',
          mode: 'cors',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' }
        });
      } catch (e) {
        console.warn('Failed to destroy session', e);
      }
    }
  }
  
  // 刷新 Session（每次上报前调用）
  private async refreshSession(): Promise<void> {
    if (!this.trackConfig) return;
    
    try {
      await fetch(`${this.trackConfig.endpoint}/api/session/refresh`, {
        method: 'POST',
        mode: 'cors',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' }
      });
    } catch (e) {
      // 刷新失败，可能是 session 失效，尝试重新 init
      if (this.userConfig && this.trackConfig) {
        await this.init(this.userConfig, this.trackConfig);
      }
    }
  }
  
  // 上报自定义事件
  track(eventId: string, properties?: Record<string, any>): void {
    if (!this.started) {
      console.warn('Tracker is not started. Call start() first.');
      return;
    }
    
    // 刷新 Session
    this.refreshSession();
    
    const event: EventData = {
      type: EventType.CUSTOM,
      eventId: eventId,
      properties: properties || {}
    };
    
    this.batchManager.addEvent(event);
  }
  
  // 自动采集的点击事件
  private captureClick(event: MouseEvent): void {
    if (!this.started) return;
    
    // 刷新 Session
    this.refreshSession();
    
    const target = event.target as HTMLElement;
    const domPath = this.getDomPath(target);
    
    const clickEvent: EventData = {
      type: EventType.CLICK,
      properties: {
        domPath: domPath,
        tagName: target.tagName,
        className: target.className,
        id: target.id,
        innerText: target.innerText?.slice(0, 100), // 限制长度
        x: event.clientX,
        y: event.clientY
      }
    };
    
    this.batchManager.addEvent(clickEvent);
  }
  
  // 获取 DOM 元素路径
  private getDomPath(element: HTMLElement): string {
    const path: string[] = [];
    let current: HTMLElement | null = element;
    
    while (current && current.nodeType === Node.ELEMENT_NODE) {
      let selector = current.nodeName.toLowerCase();
      
      if (current.id) {
        selector += `#${current.id}`;
        path.unshift(selector);
        break;
      } else {
        let sibling = current;
        let nth = 1;
        while (sibling.previousElementSibling) {
          sibling = sibling.previousElementSibling;
          if (sibling.nodeName.toLowerCase() === selector) {
            nth++;
          }
        }
        if (nth !== 1) {
          selector += `:nth-of-type(${nth})`;
        }
      }
      
      path.unshift(selector);
      current = current.parentElement;
    }
    
    return path.join(' > ');
  }
}

// 导出单例实例（推荐使用方式）
const track = Track.getInstance();

// 导出类和方法（兼容性）
export { Track, track };
export default track;
```

### 2.4 使用方式

**方式一：Vite Plugin**
```javascript
// vite.config.js
import { trackPlugin } from '@track/vite-plugin'

export default {
  plugins: [
    trackPlugin({
      endpoint: 'https://track.yourdomain.com',
      autoTrack: true,
      performance: true,
      errorTrack: true,
      sessionTTL: 1440  // 可选：Session 存活时长（分钟），默认 24 小时
    })
  ]
}

// 在业务代码中（注意：init 现在是异步的）
// Track 是单例，全局只有一个实例，防止重复初始化
await window.Track.init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: { plan: 'premium' }
}, {
  endpoint: 'https://track.yourdomain.com',
  sessionTTL: 1440  // 可选：24 小时
});
window.Track.start();

// 上报自定义事件
window.Track.track('purchase_success', { 
  productId: '123', 
  price: 99.9 
});

// 停止上报（异步，会销毁 Session）
await window.Track.stop();

// 注意：多次调用 init 会覆盖之前的配置，建议只在应用启动时调用一次
```

**方式二：Script 标签引入**
```html
<script src="https://cdn.yourdomain.com/track-sdk.js"></script>
<script>
// 必须先初始化用户信息（异步）
(async function() {
  await window.Track.init({
    appId: 'your-app-id',
    userId: 'user-123'
  }, {
    endpoint: 'https://track.yourdomain.com'
  });
  
  // 然后开始上报
  window.Track.start();
})();

// 在业务逻辑中上报自定义事件
document.getElementById('buy-btn').addEventListener('click', function() {
  window.Track.track('button_click', { 
    buttonId: 'buy-btn',
    productId: '456' 
  });
});

// 需要时停止上报（异步）
// await window.Track.stop();
</script>
```

**方式三：NPM 包**
```javascript
import track from '@track/sdk';
// 或者
import { track } from '@track/sdk';
// 或者使用类方法
import { Track } from '@track/sdk';
const track = Track.getInstance();

// 初始化用户信息（异步）
// track 是单例，全局只有一个实例，防止重复初始化
await track.init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: { role: 'admin' }
}, {
  endpoint: 'https://track.yourdomain.com',
  sessionTTL: 0  // 0 表示不过期
});

// 开始自动采集
track.start();

// 上报业务事件
track.track('user_login', { method: 'google' });

// 停止采集（异步，会销毁 Session）
// await track.stop();

// 注意：多次调用 init 会覆盖之前的配置，建议只在应用启动时调用一次
```

### 2.5 批量上报机制

#### 2.5.1 上报策略

由于单公司多项目架构，采集服务与业务项目处于不同域名，存在跨域问题。服务端必须配置 CORS 支持，客户端采用以下上报策略：

1. **优先通道**：`sendBeacon`（可靠且不阻塞页面卸载）
2. **备选通道**：`fetch POST`（支持更完善的错误处理）

**重要**：服务端必须配置 CORS，包括 `allowCredentials: true` 以支持 Cookie 传递。

```typescript
class Sender {
  private endpoint: string;
  
  async sendEvents(events: EventData[]): Promise<void> {
    // 构建 payload（不再包含用户信息，只包含事件数组）
    const payload = {
      e: events.map(e => ({
        t: e.type,
        id: e.eventId || undefined,
        p: this.removeNulls(e.properties)
      }))
    };
    
    // 策略1: sendBeacon (优先使用，页面卸载时最可靠)
    if (navigator.sendBeacon) {
      const blob = new Blob([JSON.stringify(payload)], { 
        type: 'application/json' 
      });
      if (navigator.sendBeacon(`${this.endpoint}/api/ingest`, blob)) {
        return; // 成功即返回
      }
    }
    
    // 策略2: fetch POST (支持错误处理和重试)
    try {
      const response = await fetch(`${this.endpoint}/api/ingest`, {
        method: 'POST',
        mode: 'cors',
        credentials: 'include', // 重要：允许携带 Cookie
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
      });
      
      // 处理 session 失效（401/403）
      if (response.status === 401 || response.status === 403) {
        // Session 失效，触发重新 init
        throw new Error('Session expired');
      }
      
      if (response.ok) return;
    } catch (e) {
      // 网络错误或 session 失效，保存到离线队列
      throw e;
    }
  }
}
```

#### 2.5.2 批量管理

```typescript
class BatchManager {
  private queue: EventData[] = [];
  private offlineQueue: EventData[] = [];
  private storage: Storage;
  private batchSize: number = 10;
  private batchWait: number = 5000; // 5秒
  private batchTimer: number | null = null;
  
  addEvent(event: EventData): void {
    this.queue.push(event);
    
    // 检查批次大小
    if (this.queue.length >= this.batchSize) {
      this.sendBatch();
      return;
    }
    
    // 设置定时器，超时自动发送
    if (this.batchTimer === null) {
      this.batchTimer = window.setTimeout(() => {
        this.sendBatch();
        this.batchTimer = null;
      }, this.batchWait);
    }
  }
  
  private async sendBatch(events?: EventData[]): Promise<void> {
    const eventsToSend = events || this.queue;
    if (eventsToSend.length === 0) return;
    
    // 清除定时器
    if (this.batchTimer !== null) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }
    
    try {
      await this.sender.sendEvents(eventsToSend);
      this.queue = this.queue.filter(e => !eventsToSend.includes(e));
    } catch (error) {
      // 发送失败，保存到离线队列
      if (error.message === 'Session expired') {
        // Session 失效，触发重新 init
        this.tracker.handleSessionExpired();
      } else {
        // 网络错误，保存到离线队列
        await this.storage.saveOfflineEvents(eventsToSend);
      }
    }
  }
  
  // 页面卸载时的可靠发送
  private setupUnloadHandler(): void {
    window.addEventListener('pagehide', () => {
      if (this.queue.length > 0) {
        // 尝试 sendBeacon（最可靠）
        const payload = {
          e: this.queue.map(e => ({
            t: e.type,
            id: e.eventId || undefined,
            p: this.removeNulls(e.properties)
          }))
        };
        const blob = new Blob([JSON.stringify(payload)], {
          type: 'application/json'
        });
        navigator.sendBeacon(`${this.endpoint}/api/ingest`, blob);
      }
      
      // 发送离线队列
      this.flushOfflineEvents();
    }, { capture: true });
  }
  
  // 离线队列处理
  private async flushOfflineEvents(): Promise<void> {
    const offlineEvents = await this.storage.getAllOfflineEvents();
    if (offlineEvents.length > 0) {
      try {
        await this.sender.sendEvents(offlineEvents);
        await this.storage.clearOfflineEvents();
      } catch (e) {
        // 仍然失败，保留在离线队列
      }
    }
  }
}
```

### 2.6 Examples 示例项目

在 `packages/client-sdk/examples/` 目录下提供了多个框架的集成示例：

#### 2.6.1 Vanilla JavaScript 示例

**位置**：`packages/client-sdk/examples/vanilla-js/`

**说明**：展示如何使用 script 标签引入 SDK 并集成到原生 JavaScript 项目。

**快速开始**：
```html
<!DOCTYPE html>
<html>
<head>
  <title>Track SDK - Vanilla JS Example</title>
  <script src="https://cdn.yourdomain.com/track-sdk.js"></script>
</head>
<body>
  <button id="track-btn">点击上报</button>
  
  <script>
    (async function() {
      // 初始化
      await window.Track.init({
        appId: 'your-app-id',
        userId: 'user-123'
      }, {
        endpoint: 'https://track.yourdomain.com'
      });
      
      // 开始上报
      window.Track.start();
      
      // 自定义事件
      document.getElementById('track-btn').addEventListener('click', () => {
        window.Track.track('button_click', { buttonId: 'track-btn' });
      });
    })();
  </script>
</body>
</html>
```

#### 2.6.2 Vue 3 示例

**位置**：`packages/client-sdk/examples/vue3/`

**说明**：展示如何在 Vue 3 项目中使用 Vite Plugin 和 NPM 包方式集成。

**快速开始**：
```javascript
// vite.config.js
import { trackPlugin } from '@track/vite-plugin'

export default {
  plugins: [
    trackPlugin({
      endpoint: 'https://track.yourdomain.com',
      autoTrack: true
    })
  ]
}

// main.js 或组件中
import track from '@track/sdk'

export default {
  async mounted() {
    // track 是单例，全局只有一个实例
    await track.init({
      appId: 'your-app-id',
      userId: 'user-123'
    }, {
      endpoint: 'https://track.yourdomain.com'
    })
    track.start()
  },
  methods: {
    handlePurchase() {
      track.track('purchase', { productId: '123' })
    }
  }
}
```

#### 2.6.3 Next.js 示例

**位置**：`packages/client-sdk/examples/nextjs/`

**说明**：展示如何在 Next.js 项目中处理 SSR/CSR 场景，在客户端初始化 SDK。

**快速开始**：
```typescript
// lib/track.ts
import track from '@track/sdk'

let initialized = false

export async function initTrack() {
  if (typeof window === 'undefined' || initialized) return
  
  // track 是单例，全局只有一个实例
  await track.init({
    appId: process.env.NEXT_PUBLIC_TRACK_APP_ID!,
    userId: getUserId() // 从 cookie 或其他地方获取
  }, {
    endpoint: process.env.NEXT_PUBLIC_TRACK_ENDPOINT!
  })
  
  track.start()
  initialized = true
}

export { track }

// app/layout.tsx 或 pages/_app.tsx
'use client'
import { useEffect } from 'react'
import { initTrack } from '@/lib/track'

export default function Layout({ children }) {
  useEffect(() => {
    initTrack()
  }, [])
  
  return <>{children}</>
}
```

## 3. 服务端详细需求

### 3.1 技术栈
- Spring Boot 3.x + SpringDoc OpenAPI (自动生成Swagger)
- PostgreSQL + Redis
- Docker

### 3.2 CORS 跨域配置

由于单公司多项目架构，需要配置 CORS 以支持 sendBeacon 和 fetch 方式上报。**重要**：必须配置 `allowCredentials: true` 以支持 Cookie 传递（Session 机制依赖 Cookie）。

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Session 接口（注册、刷新、销毁）
                registry.addMapping("/api/session/**")
                    .allowedOriginPatterns("https://*.yourcompany.com", "https://*.yourcompany.cn")
                    .allowedMethods("POST", "OPTIONS")
                    .allowedHeaders("Content-Type")
                    .allowCredentials(true) // 重要：必须支持 Cookie
                    .maxAge(3600);
                
                // 数据上报接口
                registry.addMapping("/api/ingest")
                    .allowedOriginPatterns("https://*.yourcompany.com", "https://*.yourcompany.cn")
                    .allowedMethods("POST", "OPTIONS")
                    .allowedHeaders("Content-Type")
                    .allowCredentials(true) // 重要：必须支持 Cookie（用于传递 sessionId）
                    .maxAge(3600);
            }
        };
    }
}
```

### 3.3 自动生成Swagger配置
```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Track API",
        version = "1.0",
        description = "埋点数据收集与分析平台"
    )
)
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

### 3.4 数据接收接口

#### 3.4.1 Session 接口

**Session 管理接口**：用于注册、刷新和销毁用户会话。

```java
@RestController
@RequestMapping("/api/session")
public class SessionController {
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @PostMapping
    @Operation(summary = "注册用户会话", description = "创建新的 session 并设置 Cookie")
    public ResponseEntity<SessionResponse> createSession(
        @RequestBody SessionRequest request,
        HttpServletResponse response) {
        
        // 验证 AppId
        if (!projectRepository.findByAppId(request.getAppId())
                .map(Project::isActive)
                .orElse(false)) {
            return ResponseEntity.badRequest().build();
        }
        
        // 生成 sessionId
        String sessionId = UUID.randomUUID().toString();
        
        // 保存到 Redis（TTL 由请求参数决定，默认 24 小时）
        int ttlMinutes = request.getTtlMinutes() != null ? request.getTtlMinutes() : 1440;
        sessionService.saveSession(sessionId, request.getAppId(), 
            request.getUserId(), request.getUserProps(), ttlMinutes);
        
        // 设置 Cookie
        Cookie cookie = new Cookie("track_session_id", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS 环境
        cookie.setMaxAge(ttlMinutes == 0 ? Integer.MAX_VALUE : ttlMinutes * 60);
        // SameSite 在 Spring Boot 中通过 CookieSameSite 配置
        response.addCookie(cookie);
        
        return ResponseEntity.ok(new SessionResponse(sessionId));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "刷新会话", description = "延长 session 过期时间")
    public ResponseEntity<Void> refreshSession(
        @CookieValue(value = "track_session_id", required = false) String sessionId,
        HttpServletRequest request,
        HttpServletResponse response) {
        
        if (sessionId == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        SessionInfo sessionInfo = sessionService.getSession(sessionId);
        if (sessionInfo == null) {
            return ResponseEntity.status(401).build(); // Session 不存在或已过期
        }
        
        // 刷新 Redis TTL（使用原始 TTL）
        sessionService.refreshSession(sessionId, sessionInfo.getTtlMinutes());
        
        // 刷新 Cookie
        Cookie cookie = new Cookie("track_session_id", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(sessionInfo.getTtlMinutes() == 0 ? Integer.MAX_VALUE : 
            sessionInfo.getTtlMinutes() * 60);
        response.addCookie(cookie);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/destroy")
    @Operation(summary = "销毁会话", description = "删除 session 并清除 Cookie")
    public ResponseEntity<Void> destroySession(
        @CookieValue(value = "track_session_id", required = false) String sessionId,
        HttpServletResponse response) {
        
        if (sessionId != null) {
            // 删除 Redis 中的 session
            sessionService.deleteSession(sessionId);
        }
        
        // 清除 Cookie
        Cookie cookie = new Cookie("track_session_id", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return ResponseEntity.ok().build();
    }
}
```

**Session 服务实现**：

```java
@Service
public class SessionService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_PREFIX = "track:session:";
    
    public void saveSession(String sessionId, String appId, String userId, 
                           Map<String, Object> userProps, int ttlMinutes) {
        SessionInfo sessionInfo = new SessionInfo(appId, userId, userProps, ttlMinutes);
        String key = SESSION_PREFIX + sessionId;
        
        if (ttlMinutes == 0) {
            // 不过期
            redisTemplate.opsForValue().set(key, sessionInfo);
        } else {
            redisTemplate.opsForValue().set(key, sessionInfo, 
                ttlMinutes, TimeUnit.MINUTES);
        }
    }
    
    public SessionInfo getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return (SessionInfo) redisTemplate.opsForValue().get(key);
    }
    
    public void refreshSession(String sessionId, int ttlMinutes) {
        SessionInfo sessionInfo = getSession(sessionId);
        if (sessionInfo == null) return;
        
        String key = SESSION_PREFIX + sessionId;
        if (ttlMinutes == 0) {
            redisTemplate.expire(key, Duration.ofDays(365 * 100)); // 近似永久
        } else {
            redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
        }
    }
    
    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
```

#### 3.4.2 数据接收接口

```java
@RestController
public class TrackController {
    
    @Autowired
    private TrackService trackService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @PostMapping("/api/ingest")
    @Operation(summary = "接收埋点数据", description = "通过POST请求接收埋点数据，需要CORS支持")
    public ResponseEntity<Void> trackEvent(
        @RequestBody TrackBatchRequest batchRequest,
        @CookieValue(value = "track_session_id", required = false) String sessionId,
        HttpServletRequest request) {
        
        try {
            // 1. 从 Cookie 获取 sessionId
            if (sessionId == null) {
                return ResponseEntity.status(401).build(); // Unauthorized
            }
            
            // 2. 从 Redis 获取用户信息
            SessionInfo sessionInfo = sessionService.getSession(sessionId);
            if (sessionInfo == null) {
                return ResponseEntity.status(401).build(); // Session 不存在或已过期
            }
            
            // 3. 验证 AppId（单公司多项目：验证项目是否存在且激活）
            if (!trackService.validateAppId(sessionInfo.getAppId())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 4. 限流检查
            String clientIp = getClientIp(request);
            if (!rateLimitService.isAllowed(sessionInfo.getAppId(), clientIp)) {
                return ResponseEntity.status(429).build(); // Too Many Requests
            }
            
            // 5. 补充信息（从 session 和请求中获取）
            batchRequest.setAppId(sessionInfo.getAppId());
            batchRequest.setUserId(sessionInfo.getUserId());
            batchRequest.setUserProps(sessionInfo.getUserProps());
            batchRequest.setServerTimestamp(LocalDateTime.now());
            batchRequest.setIpAddress(clientIp);
            batchRequest.setUserAgent(request.getHeader("User-Agent"));
            
            // 6. 异步处理（不阻塞响应）
            trackService.processBatchEventsAsync(batchRequest);
            
            return ResponseEntity.accepted().build(); // 202 Accepted
            
        } catch (Exception e) {
            log.error("Track event processing error", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

#### 3.4.3 限流服务

```java
@Service
public class RateLimitService {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    // 按IP限流
    public boolean isAllowed(String ip) {
        String key = "rate:ip:" + ip;
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, 60, TimeUnit.SECONDS);
        }
        return count <= 1000; // 每分钟最多1000次
    }
    
    // 按AppId限流（单公司多项目场景）
    public boolean isAllowed(String appId, String ip) {
        // IP限流
        if (!isAllowed(ip)) {
            return false;
        }
        
        // AppId限流
        String appKey = "rate:app:" + appId;
        Long count = redis.opsForValue().increment(appKey);
        if (count == 1) {
            redis.expire(appKey, 60, TimeUnit.SECONDS);
        }
        return count <= 5000; // 每个AppId每分钟最多5000次
    }
}
```

#### 3.4.4 AppId 验证服务

```java
@Service
public class TrackService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    public boolean validateAppId(String appId) {
        return projectRepository.findByAppId(appId)
            .map(Project::isActive)
            .orElse(false);
    }
    
    @Async
    public void processBatchEventsAsync(TrackBatchRequest request) {
        // 验证AppId（从 Session 中获取的 appId）
        Project project = projectRepository.findByAppId(request.getAppId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid appId"));
        
        if (!project.isActive()) {
            log.warn("Attempt to track inactive project: {}", request.getAppId());
            return;
        }
        
        // 处理事件...
        processBatchEvents(request);
    }
    
    private void processBatchEvents(TrackBatchRequest request) {
        List<Event> events = new ArrayList<>();
        
        for (TrackBatchRequest.EventDTO eventDTO : request.getE()) {
            // 将客户端传来的数字类型转换为枚举
            EventType eventType = EventType.fromCode(eventDTO.getT());
            
            Event event = new Event();
            event.setAppId(request.getAppId());
            event.setUserId(request.getUserId());
            event.setUserProperties(request.getUserProps());
            event.setEventTypeId((short) eventType.getCode()); // 存储枚举的 code 值
            event.setCustomEventId(eventDTO.getId());
            event.setProperties(eventDTO.getP());
            
            // 如果是点击事件，提取 DOM 路径
            if (eventType == EventType.CLICK && eventDTO.getP() != null) {
                event.setDomPath((String) eventDTO.getP().get("domPath"));
            }
            
            event.setPageUrl(request.getPageUrl());
            event.setPageTitle(request.getPageTitle());
            event.setReferrer(request.getReferrer());
            event.setUserAgent(request.getUserAgent());
            event.setIpAddress(request.getIpAddress());
            event.setServerTimestamp(request.getServerTimestamp());
            
            events.add(event);
        }
        
        eventRepository.saveAll(events);
    }
}
```

### 3.5 数据模型设计

#### 3.5.1 数据库表结构

```sql
-- 事件类型枚举表
-- 注意：ID 值必须与客户端和服务端枚举值保持一致（见 2.2 节）
CREATE TABLE event_types (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);

-- 预置事件类型
-- 注意：ID 值必须与客户端 EventType 枚举和服务端 EventType 枚举完全一致
INSERT INTO event_types (id, name, description) VALUES
(1, 'page_view', '页面浏览'),      -- 对应 EventType.PAGE_VIEW = 1
(2, 'click', '点击事件'),          -- 对应 EventType.CLICK = 2
(3, 'performance', '性能指标'),    -- 对应 EventType.PERFORMANCE = 3
(4, 'error', '错误监控'),          -- 对应 EventType.ERROR = 4
(5, 'custom', '自定义事件'),       -- 对应 EventType.CUSTOM = 5
(6, 'page_stay', '页面停留');      -- 对应 EventType.PAGE_STAY = 6

-- 事件数据表
-- 注意：app_id、user_id、user_properties 从 Session 中获取，不再从请求体获取
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    user_properties JSONB,
    event_type_id SMALLINT NOT NULL REFERENCES event_types(id),
    custom_event_id VARCHAR(128),  -- 自定义事件唯一标识符
    properties JSONB,              -- 事件属性
    dom_path TEXT,                 -- DOM路径（点击事件）
    page_url TEXT,
    page_title TEXT,
    referrer TEXT,
    user_agent TEXT,
    ip_address INET,
    server_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX idx_events_app_user ON events(app_id, user_id);
CREATE INDEX idx_events_timestamp ON events(server_timestamp);
CREATE INDEX idx_events_type ON events(event_type_id);
CREATE INDEX idx_events_custom_id ON events(custom_event_id) WHERE custom_event_id IS NOT NULL;

-- 项目表
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(64) UNIQUE NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    description TEXT,
    created_by BIGINT REFERENCES users(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 用户表（管理后台用户）
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 项目成员表
CREATE TABLE project_members (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    user_id BIGINT REFERENCES users(id),
    role VARCHAR(20) DEFAULT 'member',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);
```

#### 3.5.2 Session 数据存储（Redis）

**存储结构**：
- Key: `track:session:{sessionId}`
- Value: JSON 格式的 `SessionInfo` 对象
- TTL: 由 `sessionTTL` 配置决定（默认 24 小时，0 表示不过期）

**SessionInfo 结构**：
```java
public class SessionInfo {
    private String appId;
    private String userId;
    private Map<String, Object> userProps;
    private int ttlMinutes; // 原始 TTL，用于刷新时保持一致性
}
```

**Redis 存储示例**：
```json
{
  "appId": "your-app-id",
  "userId": "user-123",
  "userProps": {
    "plan": "premium",
    "role": "admin"
  },
  "ttlMinutes": 1440
}
```

### 3.6 DTO 设计

#### 3.6.1 Session 相关 DTO

```java
// Session 注册请求
public class SessionRequest {
    private String appId;
    private String userId;
    private Map<String, Object> userProps;
    private Integer ttlMinutes; // 可选，默认 1440（24小时），0 表示不过期
}

// Session 响应
public class SessionResponse {
    private String sessionId;
}

// Session 信息（存储在 Redis）
public class SessionInfo {
    private String appId;
    private String userId;
    private Map<String, Object> userProps;
    private int ttlMinutes; // 原始 TTL
}
```

#### 3.6.2 数据上报 DTO

```java
// 批量请求DTO（客户端发送，不再包含用户信息）
public class TrackBatchRequest {
    // 注意：不再包含 a(appId)、u(userId)、up(userProps)
    // 这些信息从 Cookie 中的 sessionId 获取
    private List<EventDTO> e; // events
    
    @Setter @Getter
    public static class EventDTO {
        /**
         * 事件类型（对应 EventType 枚举的 code 值）
         * 1: PAGE_VIEW, 2: CLICK, 3: PERFORMANCE, 4: ERROR, 5: CUSTOM, 6: PAGE_STAY
         * 注意：必须与客户端 EventType 枚举值保持一致
         */
        private Integer t; // type
        private String id; // custom event id
        private Map<String, Object> p; // properties
    }
    
    // 服务端补充字段（从 Session 和请求中获取）
    private String appId;      // 从 Session 获取
    private String userId;     // 从 Session 获取
    private Map<String, Object> userProps; // 从 Session 获取
    private LocalDateTime serverTimestamp;
    private String ipAddress;
    private String userAgent;
}

// 事件实体
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "app_id", nullable = false)
    private String appId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "user_properties")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> userProperties;
    
    /**
     * 事件类型ID（对应 EventType 枚举的 code 值）
     * 注意：必须与客户端 EventType 枚举值保持一致
     * 1: PAGE_VIEW, 2: CLICK, 3: PERFORMANCE, 4: ERROR, 5: CUSTOM, 6: PAGE_STAY
     */
    @Column(name = "event_type_id", nullable = false)
    private Short eventTypeId;
    
    /**
     * 获取事件类型枚举
     */
    public EventType getEventType() {
        return EventType.fromCode(this.eventTypeId);
    }
    
    /**
     * 设置事件类型枚举
     */
    public void setEventType(EventType eventType) {
        this.eventTypeId = (short) eventType.getCode();
    }
    
    @Column(name = "custom_event_id")
    private String customEventId;
    
    @Column(name = "properties")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> properties;
    
    @Column(name = "dom_path")
    private String domPath;
    
    @Column(name = "page_url")
    private String pageUrl;
    
    @Column(name = "page_title")
    private String pageTitle;
    
    @Column(name = "referrer")
    private String referrer;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    
    @Column(name = "server_timestamp", nullable = false)
    private LocalDateTime serverTimestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

## 4. 数据分析平台详细需求

### 4.1 功能模块

#### 4.1.1 用户权限管理
- 用户登录/登出
- 角色权限控制（超级管理员、项目管理员、成员）
- AppId 项目管理
- 项目成员管理

#### 4.1.2 数据大屏
- 实时数据监控
- 核心指标展示（PV、UV、跳出率、平均时长）
- 流量趋势图表
- 事件类型分布
- 用户行为分析

**PV/UV 统计说明**：
- **PV (Page View)**：页面浏览量，统计 `event_type_id = 1` (PAGE_VIEW) 的事件总数
- **UV (Unique Visitor)**：独立访客数，统计 `event_type_id = 1` (PAGE_VIEW) 事件中不同 `user_id` 的数量
- 统计维度：可按 `app_id`、`page_url`、时间范围等维度进行统计

#### 4.1.3 分析功能
- **事件分析**：自定义事件查询、统计（按 eventId 分组）
- **用户分析**：用户路径、会话分析
- **点击热图**：基于 DOM 路径的点击分布
- **性能监控**：页面加载性能指标
- **错误分析**：JavaScript 错误统计

### 4.2 技术实现

#### 4.2.1 PV/UV 统计实现

**客户端自动采集 PV**：
- 当调用 `start()` 后，SDK 会自动监听页面加载和路由变化
- 每次页面加载时自动上报 `PAGE_VIEW` 事件（`event_type_id = 1`）
- 支持 SPA 路由变化检测（pushState/replaceState/hashchange/popstate）
- 事件包含：`page_url`、`page_title`、`referrer` 等信息

**客户端实现示例**：

```typescript
class Track {
  // 设置自动采集
  private setupAutoTrack(): void {
    // 监听页面加载（首次加载）
    if (document.readyState === 'complete') {
      this.trackPageView();
    } else {
      window.addEventListener('load', () => this.trackPageView());
    }
    
    // 监听 SPA 路由变化（pushState/replaceState）
    this.interceptHistoryMethods();
    
    // 监听 hash 变化（hash 路由）
    window.addEventListener('hashchange', () => this.trackPageView());
    
    // 监听 popstate（浏览器前进/后退）
    window.addEventListener('popstate', () => this.trackPageView());
  }
  
  // 上报页面浏览事件（PV）
  private trackPageView(): void {
    if (!this.started) return;
    
    // 刷新 Session
    this.refreshSession();
    
    const pageViewEvent: EventData = {
      type: EventType.PAGE_VIEW,
      properties: {
        pageUrl: window.location.href,
        pageTitle: document.title,
        referrer: document.referrer,
        timestamp: Date.now()
      }
    };
    
    this.batchManager.addEvent(pageViewEvent);
  }
  
  // 拦截 History API（用于 SPA 路由变化检测）
  private interceptHistoryMethods(): void {
    const originalPushState = history.pushState;
    const originalReplaceState = history.replaceState;
    
    history.pushState = (...args) => {
      originalPushState.apply(history, args);
      setTimeout(() => this.trackPageView(), 0);
    };
    
    history.replaceState = (...args) => {
      originalReplaceState.apply(history, args);
      setTimeout(() => this.trackPageView(), 0);
    };
  }
}
```

**服务端统计逻辑**：

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    /**
     * 统计 PV（页面浏览量）
     * 统计规则：统计指定时间范围内 event_type_id = 1 (PAGE_VIEW) 的事件总数
     */
    public long getPV(String appId, LocalDateTime startTime, LocalDateTime endTime, String pageUrl) {
        Specification<Event> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("appId"), appId)
        ).and((root, query, cb) -> 
            cb.equal(root.get("eventTypeId"), EventType.PAGE_VIEW.getCode())
        ).and((root, query, cb) -> 
            cb.between(root.get("serverTimestamp"), startTime, endTime)
        );
        
        if (pageUrl != null && !pageUrl.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("pageUrl"), pageUrl)
            );
        }
        
        return eventRepository.count(spec);
    }
    
    /**
     * 统计 UV（独立访客数）
     * 统计规则：统计指定时间范围内 event_type_id = 1 (PAGE_VIEW) 事件中不同 user_id 的数量
     */
    public long getUV(String appId, LocalDateTime startTime, LocalDateTime endTime, String pageUrl) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Event> root = query.from(Event.class);
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("appId"), appId));
        predicates.add(cb.equal(root.get("eventTypeId"), EventType.PAGE_VIEW.getCode()));
        predicates.add(cb.between(root.get("serverTimestamp"), startTime, endTime));
        
        if (pageUrl != null && !pageUrl.isEmpty()) {
            predicates.add(cb.equal(root.get("pageUrl"), pageUrl));
        }
        
        query.select(cb.countDistinct(root.get("userId")))
             .where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getSingleResult();
    }
}
```

**SQL 统计示例**：

```sql
-- 统计 PV（页面浏览量）
-- 统计指定时间范围内的 PAGE_VIEW 事件总数
SELECT COUNT(*) as pv
FROM events
WHERE app_id = 'your-app-id'
  AND event_type_id = 1  -- PAGE_VIEW
  AND server_timestamp BETWEEN '2024-01-01 00:00:00' AND '2024-01-31 23:59:59';

-- 统计 UV（独立访客数）
-- 统计指定时间范围内访问过的不同用户数
SELECT COUNT(DISTINCT user_id) as uv
FROM events
WHERE app_id = 'your-app-id'
  AND event_type_id = 1  -- PAGE_VIEW
  AND server_timestamp BETWEEN '2024-01-01 00:00:00' AND '2024-01-31 23:59:59';

-- 按页面统计 PV/UV
SELECT 
    page_url,
    COUNT(*) as pv,
    COUNT(DISTINCT user_id) as uv
FROM events
WHERE app_id = 'your-app-id'
  AND event_type_id = 1  -- PAGE_VIEW
  AND server_timestamp BETWEEN '2024-01-01 00:00:00' AND '2024-01-31 23:59:59'
GROUP BY page_url
ORDER BY pv DESC;

-- 按日期统计 PV/UV 趋势
SELECT 
    DATE(server_timestamp) as date,
    COUNT(*) as pv,
    COUNT(DISTINCT user_id) as uv
FROM events
WHERE app_id = 'your-app-id'
  AND event_type_id = 1  -- PAGE_VIEW
  AND server_timestamp BETWEEN '2024-01-01 00:00:00' AND '2024-01-31 23:59:59'
GROUP BY DATE(server_timestamp)
ORDER BY date;
```

#### 4.2.2 其他分析功能

```typescript
// API服务
class AnalyticsService {
  // 获取 PV/UV 统计
  async getPVUV(appId: string, startTime: string, endTime: string, pageUrl?: string) {
    return request.get('/api/analytics/pv-uv', {
      params: { appId, startTime, endTime, pageUrl }
    });
  }
  
  // 获取点击热图数据
  async getClickHeatmap(appId: string, startTime: string, endTime: string) {
    return request.get('/api/analytics/click-heatmap', {
      params: { appId, startTime, endTime }
    });
  }
  
  // 获取自定义事件统计
  async getCustomEvents(appId: string, eventId?: string) {
    return request.get('/api/analytics/custom-events', {
      params: { appId, eventId }
    });
  }
  
  // 获取用户行为序列
  async getUserBehavior(appId: string, userId: string) {
    return request.get('/api/analytics/user-behavior', {
      params: { appId, userId }
    });
  }
}
```

## 5. 测试策略

### 5.1 客户端SDK测试
```typescript
describe('Track SDK', () => {
  let track: Track;
  
  beforeEach(() => {
    // 获取单例实例
    track = Track.getInstance();
    // 重置状态（测试环境需要）
    track['initialized'] = false;
    track['started'] = false;
    track['userConfig'] = null;
    track['trackConfig'] = null;
  });
  
  it('should be a singleton', () => {
    const instance1 = Track.getInstance();
    const instance2 = Track.getInstance();
    expect(instance1).toBe(instance2);
  });
  
  it('should prevent direct instantiation', () => {
    // TypeScript 中私有构造函数会阻止 new，在运行时需要额外检查
    // 在 JavaScript 运行时，可以通过设置 constructor 检查
    expect(() => {
      // 尝试绕过 TypeScript 检查
      const TrackClass = Track as any;
      new TrackClass();
    }).toThrow();
  });
  
  it('should require init before start', () => {
    const track = Track.getInstance();
    expect(() => track.start()).toThrow('Must call init() before start()');
  });
  
  it('should initialize user config and register session', async () => {
    const track = Track.getInstance();
    await track.init({ appId: 'test', userId: 'user1' });
    expect(track['initialized']).toBe(true);
  });
  
  it('should start and stop tracking', async () => {
    const track = Track.getInstance();
    await track.init({ appId: 'test', userId: 'user1' });
    track.start();
    expect(track['started']).toBe(true);
    
    await track.stop();
    expect(track['started']).toBe(false);
  });
  
  it('should capture DOM path for click events', () => {
    const track = Track.getInstance();
    const element = document.createElement('button');
    element.id = 'test-btn';
    element.className = 'btn primary';
    document.body.appendChild(element);
    
    const domPath = track['getDomPath'](element);
    expect(domPath).toContain('button#test-btn');
  });
  
  it('should track custom events with eventId', async () => {
    const track = Track.getInstance();
    await track.init({ appId: 'test', userId: 'user1' });
    track.start();
    
    const addEventSpy = jest.spyOn(track['batchManager'], 'addEvent');
    track.track('purchase_success', { amount: 100 });
    
    expect(addEventSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        type: EventType.CUSTOM,
        eventId: 'purchase_success',
        properties: { amount: 100 }
      })
    );
  });
});
```

### 5.2 服务端测试
```java
@SpringBootTest
class TrackServiceTest {
    
    @Autowired
    private TrackService trackService;
    
    @MockBean
    private EventRepository eventRepository;
    
    @Test
    void shouldProcessBatchEventsWithSessionInfo() {
        // 模拟从 Session 获取用户信息
        String sessionId = "test-session-id";
        SessionInfo sessionInfo = new SessionInfo("test-app", "user-123", 
            Map.of("plan", "premium"), 1440);
        when(sessionService.getSession(sessionId)).thenReturn(sessionInfo);
        
        TrackBatchRequest request = new TrackBatchRequest();
        // 注意：请求体不再包含用户信息，只有事件数组
        TrackBatchRequest.EventDTO event = new TrackBatchRequest.EventDTO();
        event.setT(5); // CUSTOM
        event.setId("purchase");
        event.setP(Map.of("product", "laptop"));
        request.setE(List.of(event));
        
        // 服务端会从 Session 中获取用户信息并设置到 request
        request.setAppId(sessionInfo.getAppId());
        request.setUserId(sessionInfo.getUserId());
        request.setUserProps(sessionInfo.getUserProps());
        
        trackService.processBatchEvents(request);
        
        verify(eventRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void shouldHandleClickEventsWithDomPath() {
        // 模拟从 Session 获取用户信息
        String sessionId = "test-session-id";
        SessionInfo sessionInfo = new SessionInfo("test-app", "user-123", 
            Map.of(), 1440);
        when(sessionService.getSession(sessionId)).thenReturn(sessionInfo);
        
        TrackBatchRequest request = new TrackBatchRequest();
        // 注意：请求体不再包含用户信息
        TrackBatchRequest.EventDTO event = new TrackBatchRequest.EventDTO();
        event.setT(2); // CLICK
        event.setP(Map.of(
            "domPath", "html>body>div>button",
            "tagName", "BUTTON",
            "className", "btn-primary"
        ));
        request.setE(List.of(event));
        
        // 服务端从 Session 获取用户信息
        request.setAppId(sessionInfo.getAppId());
        request.setUserId(sessionInfo.getUserId());
        
        trackService.processBatchEvents(request);
        
        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        
        Event savedEvent = captor.getValue().get(0);
        assertThat(savedEvent.getDomPath()).isEqualTo("html>body>div>button");
    }
}
```

## 6. 部署方案

### 6.1 Docker Compose配置
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: track
      POSTGRES_USER: track
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  server:
    build: ./packages/server
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/track
      SPRING_DATASOURCE_USERNAME: track
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres
    restart: unless-stopped

  dashboard:
    build: ./packages/dashboard
    ports:
      - "3000:3000"
    depends_on:
      - server
    restart: unless-stopped

volumes:
  postgres_data:
```

### 6.2 环境配置
```bash
# .env 文件
DB_PASSWORD=your_secure_password_here
JWT_SECRET=your_jwt_secret_here
TRACK_ENDPOINT=https://track.yourdomain.com
```
