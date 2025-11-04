# Track - 企业级开源埋点项目

## 项目概述

Track 是一个轻量级企业级开源埋点数据分析平台，提供从数据采集、存储到分析的全链路解决方案。

### 架构特点

- **单公司多项目设计**：支持一个公司管理多个业务项目（AppId），每个项目独立的数据采集与分析
- **跨域数据采集**：采用 GIF 图片请求方式实现跨域数据上报，适配不同业务域名场景
- **多通道上报策略**：优先使用 sendBeacon/fetch，失败时自动降级到 GIF 方案

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
- **初始化机制**：必须先调用 `init` 初始化用户信息，才能调用 `start` 开始上报
- **中断上报**：支持 `stop` 方法暂停数据上报
- **自动采集**：
  - PV/UV 统计
  - 用户点击行为（包含完整 DOM 结构信息）
  - 页面性能指标
  - JavaScript 错误监控
  - 页面停留时长
- **自定义事件**：支持唯一标识符区分不同自定义事件

### 2.2 事件类型枚举
```typescript
// 客户端与服务端约定的事件类型枚举（使用数字减少传输长度）
enum EventType {
  PAGE_VIEW = 1,      // 页面浏览
  CLICK = 2,          // 点击事件
  PERFORMANCE = 3,    // 性能指标
  ERROR = 4,          // 错误监控
  CUSTOM = 5,         // 自定义事件
  PAGE_STAY = 6       // 页面停留
}
```

### 2.3 API 设计

```typescript
interface UserConfig {
  appId: string;
  userId: string;
  userProps?: Record<string, any>;
}

interface TrackConfig {
  endpoint: string;
  autoTrack?: boolean;
  performance?: boolean;
  errorTrack?: boolean;
  batchSize?: number;
  batchWait?: number;
  debug?: boolean;
}

class Track {
  private initialized: boolean = false;
  private started: boolean = false;
  private userConfig: UserConfig | null = null;
  
  // 初始化用户信息（必须先调用）
  init(userConfig: UserConfig, trackConfig?: TrackConfig): void {
    this.userConfig = userConfig;
    this.initialized = true;
    // 存储配置信息
    this.storage.saveUserConfig(userConfig);
    this.storage.saveTrackConfig(trackConfig || {});
  }
  
  // 开始上报（必须在 init 后调用）
  start(): void {
    if (!this.initialized) {
      throw new Error('Must call init() before start()');
    }
    this.started = true;
    this.setupAutoTrack();
  }
  
  // 停止上报
  stop(): void {
    this.started = false;
    this.removeAutoTrack();
  }
  
  // 上报自定义事件
  track(eventId: string, properties?: Record<string, any>): void {
    if (!this.started) {
      console.warn('Tracker is not started. Call start() first.');
      return;
    }
    
    const event: EventData = {
      type: EventType.CUSTOM,
      eventId: eventId,  // 自定义事件唯一标识符
      properties: properties || {},
      user: this.userConfig!
    };
    
    this.batchManager.addEvent(event);
  }
  
  // 自动采集的点击事件
  private captureClick(event: MouseEvent): void {
    if (!this.started) return;
    
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
      },
      user: this.userConfig!
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
      errorTrack: true
    })
  ]
}

// 在业务代码中
window.Track.init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: { plan: 'premium' }
});
window.Track.start();

// 上报自定义事件
window.Track.track('purchase_success', { 
  productId: '123', 
  price: 99.9 
});

// 停止上报
window.Track.stop();
```

**方式二：Script 标签引入**
```html
<script src="https://cdn.yourdomain.com/track-sdk.js"></script>
<script>
// 必须先初始化用户信息
window.Track.init({
  appId: 'your-app-id',
  userId: 'user-123'
});

// 然后开始上报
window.Track.start();

// 在业务逻辑中上报自定义事件
document.getElementById('buy-btn').addEventListener('click', function() {
  window.Track.track('button_click', { 
    buttonId: 'buy-btn',
    productId: '456' 
  });
});

// 需要时停止上报
// window.Track.stop();
</script>
```

**方式三：NPM 包**
```javascript
import { init, start, track, stop } from '@track/sdk';

// 初始化用户信息
init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: { role: 'admin' }
});

// 开始自动采集
start();

// 上报业务事件
track('user_login', { method: 'google' });

// 停止采集
// stop();
```

### 2.5 批量上报机制

#### 2.5.1 多通道上报策略

由于单公司多项目架构，采集服务与业务项目处于不同域名，存在跨域问题。采用多通道上报策略：

1. **优先通道**：`sendBeacon`（如果服务端配置了 CORS）
2. **备选通道**：`fetch POST`（如果服务端配置了 CORS）
3. **兜底通道**：GIF 图片请求（天然跨域，无需 CORS）

```typescript
class Sender {
  private endpoint: string;
  
  async sendEvents(events: EventData[]): Promise<void> {
    const payload = this.buildPayload(events);
    
    // 策略1: sendBeacon (如果可用且配置了CORS)
    if (navigator.sendBeacon) {
      const blob = new Blob([JSON.stringify(payload)], { 
        type: 'application/json' 
      });
      if (navigator.sendBeacon(`${this.endpoint}/api/ingest`, blob)) {
        return; // 成功即返回
      }
    }
    
    // 策略2: fetch POST (配置了CORS)
    try {
      const response = await fetch(`${this.endpoint}/api/ingest`, {
        method: 'POST',
        mode: 'cors',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
      });
      if (response.ok) return;
    } catch (e) {
      // CORS失败或网络错误，降级到GIF
    }
    
    // 策略3: GIF 图片（兜底，无跨域限制）
    this.sendByGif(payload);
  }
}
```

#### 2.5.2 批量管理与 URL 长度控制

```typescript
class BatchManager {
  private queue: EventData[] = [];
  private maxUrlLength = 2000; // 保守值，兼容IE限制
  private offlineQueue: EventData[] = [];
  private storage: Storage;
  
  addEvent(event: EventData): void {
    this.queue.push(event);
    
    // 检查批次大小
    if (this.queue.length >= this.batchSize) {
      this.sendBatch();
      return;
    }
    
    // 检查URL长度（GIF方式）
    const testUrl = this.buildGifUrl(this.queue);
    if (testUrl.length > this.maxUrlLength) {
      // 发送当前队列（排除最后一个事件）
      const eventsToSend = this.queue.slice(0, -1);
      this.queue = [event]; // 保留最后一个事件
      this.sendBatch(eventsToSend);
    }
  }
  
  private async sendBatch(events?: EventData[]): Promise<void> {
    const eventsToSend = events || this.queue;
    if (eventsToSend.length === 0) return;
    
    try {
      await this.sender.sendEvents(eventsToSend);
      this.queue = this.queue.filter(e => !eventsToSend.includes(e));
    } catch (error) {
      // 发送失败，保存到离线队列
      await this.storage.saveOfflineEvents(eventsToSend);
    }
  }
  
  private buildGifUrl(events: EventData[]): string {
    const payload = {
      a: events[0].user.appId,
      u: events[0].user.userId,
      up: this.removeNulls(events[0].user.userProps),
      e: events.map(e => ({
        t: e.type,
        id: e.eventId || undefined,
        p: this.removeNulls(e.properties)
      }))
    };
    const encoded = btoa(unescape(encodeURIComponent(JSON.stringify(payload))));
    return `${this.endpoint}/track.gif?d=${encodeURIComponent(encoded)}`;
  }
  
  private sendByGif(payload: object): void {
    const data = btoa(unescape(encodeURIComponent(JSON.stringify(payload))));
    const url = `${this.endpoint}/track.gif?d=${encodeURIComponent(data)}`;
    
    // URL长度检查
    if (url.length > this.maxUrlLength) {
      // 分片发送或仅发送关键信息
      this.splitAndSend(payload);
      return;
    }
    
    const img = new Image();
    img.referrerPolicy = 'no-referrer';
    img.src = url;
    
    // 错误重试机制
    img.onerror = () => {
      this.retryWithBackoff(() => this.sendByGif(payload));
    };
  }
  
  // 页面卸载时的可靠发送
  private setupUnloadHandler(): void {
    window.addEventListener('pagehide', () => {
      if (this.queue.length > 0) {
        // 尝试sendBeacon（最可靠）
        const blob = new Blob([JSON.stringify(this.queue)], {
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

## 3. 服务端详细需求

### 3.1 技术栈
- Spring Boot 3.x + SpringDoc OpenAPI (自动生成Swagger)
- PostgreSQL + Redis
- Docker

### 3.2 CORS 跨域配置

由于单公司多项目架构，需要配置 CORS 以支持 sendBeacon 和 fetch 方式上报。

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // 允许公司所有子域名跨域访问（单公司多项目场景）
                registry.addMapping("/api/ingest")
                    .allowedOriginPatterns("https://*.yourcompany.com", "https://*.yourcompany.cn")
                    .allowedMethods("POST", "OPTIONS")
                    .allowedHeaders("Content-Type")
                    .allowCredentials(false) // 不传cookie，更安全
                    .maxAge(3600);
                
                // GIF接口不需要CORS（图片天然跨域）
                // 但会在响应头中设置缓存控制
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

#### 3.4.1 GIF 接口（兜底方案）

```java
@RestController
public class TrackController {
    
    private static final byte[] TRANSPARENT_GIF = new byte[] { 
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
        0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0x21,
        0xF9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
        0x01, 0x00, 0x3B
    };
    
    @Autowired
    private TrackService trackService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @GetMapping("/track.gif")
    @Operation(summary = "接收埋点数据（GIF）", description = "通过图片请求接收埋点数据，无跨域限制")
    public ResponseEntity<byte[]> trackEvent(
        @Parameter(description = "编码后的埋点数据") @RequestParam String d,
        HttpServletRequest request) {
        
        // 1. 设置缓存控制（重要！防止浏览器缓存）
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        
        try {
            // 2. URL长度检查（服务端二次验证）
            if (d.length() > 5000) {
                log.warn("Track payload too large: {}", d.length());
                return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_GIF)
                    .body(TRANSPARENT_GIF);
            }
            
            // 3. 限流检查（按IP）
            String clientIp = getClientIp(request);
            if (!rateLimitService.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_GIF)
                    .body(TRANSPARENT_GIF);
            }
            
            // 4. 解码数据
            String jsonData = new String(Base64.getDecoder().decode(d));
            TrackBatchRequest batchRequest = objectMapper.readValue(jsonData, TrackBatchRequest.class);
            
            // 5. 验证AppId（单公司多项目：验证项目是否存在且激活）
            if (!trackService.validateAppId(batchRequest.getA())) {
                log.warn("Invalid or inactive appId: {}", batchRequest.getA());
                return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_GIF)
                    .body(TRANSPARENT_GIF);
            }
            
            // 6. 服务端补充信息
            batchRequest.setServerTimestamp(LocalDateTime.now());
            batchRequest.setIpAddress(clientIp);
            batchRequest.setUserAgent(request.getHeader("User-Agent"));
            
            // 7. 异步处理（不阻塞响应）
            trackService.processBatchEventsAsync(batchRequest);
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.IMAGE_GIF)
                .body(TRANSPARENT_GIF);
                
        } catch (Exception e) {
            log.error("Track event processing error", e);
            // 即使错误也返回GIF，避免图片加载失败
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.IMAGE_GIF)
                .body(TRANSPARENT_GIF);
        }
    }
    
    @PostMapping("/api/ingest")
    @Operation(summary = "接收埋点数据（JSON）", description = "通过POST请求接收埋点数据，需要CORS支持")
    public ResponseEntity<Void> trackEventJson(
        @RequestBody TrackBatchRequest batchRequest,
        HttpServletRequest request) {
        
        try {
            // 验证AppId
            if (!trackService.validateAppId(batchRequest.getA())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 限流检查
            String clientIp = getClientIp(request);
            if (!rateLimitService.isAllowed(batchRequest.getA(), clientIp)) {
                return ResponseEntity.status(429).build(); // Too Many Requests
            }
            
            // 补充信息
            batchRequest.setServerTimestamp(LocalDateTime.now());
            batchRequest.setIpAddress(clientIp);
            batchRequest.setUserAgent(request.getHeader("User-Agent"));
            
            // 异步处理
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

#### 3.4.2 限流服务

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

#### 3.4.3 AppId 验证服务

```java
@Service
public class TrackService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    public boolean validateAppId(String appId) {
        return projectRepository.findByAppId(appId)
            .map(Project::isActive)
            .orElse(false);
    }
    
    @Async
    public void processBatchEventsAsync(TrackBatchRequest request, HttpServletRequest httpRequest) {
        // 验证AppId
        Project project = projectRepository.findByAppId(request.getA())
            .orElseThrow(() -> new IllegalArgumentException("Invalid appId"));
        
        if (!project.isActive()) {
            log.warn("Attempt to track inactive project: {}", request.getA());
            return;
        }
        
        // 处理事件...
        processBatchEvents(request);
    }
}
```

### 3.5 数据模型设计
```sql
-- 事件类型枚举表
CREATE TABLE event_types (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);

-- 预置事件类型
INSERT INTO event_types (id, name, description) VALUES
(1, 'page_view', '页面浏览'),
(2, 'click', '点击事件'),
(3, 'performance', '性能指标'),
(4, 'error', '错误监控'),
(5, 'custom', '自定义事件'),
(6, 'page_stay', '页面停留');

-- 事件数据表
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

### 3.6 DTO 设计
```java
// 批量请求DTO
public class TrackBatchRequest {
    private String a; // appId
    private String u; // userId
    private Map<String, Object> up; // userProps
    private List<EventDTO> e; // events
    
    @Setter @Getter
    public static class EventDTO {
        private Integer t; // type
        private String id; // custom event id
        private Map<String, Object> p; // properties
    }
    
    // 服务端补充字段
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
    
    @Column(name = "event_type_id", nullable = false)
    private Short eventTypeId;
    
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

#### 4.1.3 分析功能
- **事件分析**：自定义事件查询、统计（按 eventId 分组）
- **用户分析**：用户路径、会话分析
- **点击热图**：基于 DOM 路径的点击分布
- **性能监控**：页面加载性能指标
- **错误分析**：JavaScript 错误统计

### 4.2 技术实现
```typescript
// API服务
class AnalyticsService {
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
    track = new Track();
  });
  
  it('should require init before start', () => {
    expect(() => track.start()).toThrow('Must call init() before start()');
  });
  
  it('should initialize user config', () => {
    track.init({ appId: 'test', userId: 'user1' });
    expect(track['initialized']).toBe(true);
  });
  
  it('should start and stop tracking', () => {
    track.init({ appId: 'test', userId: 'user1' });
    track.start();
    expect(track['started']).toBe(true);
    
    track.stop();
    expect(track['started']).toBe(false);
  });
  
  it('should capture DOM path for click events', () => {
    const element = document.createElement('button');
    element.id = 'test-btn';
    element.className = 'btn primary';
    document.body.appendChild(element);
    
    const domPath = track['getDomPath'](element);
    expect(domPath).toContain('button#test-btn');
  });
  
  it('should track custom events with eventId', () => {
    track.init({ appId: 'test', userId: 'user1' });
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
    void shouldProcessBatchEventsWithUserInfo() {
        TrackBatchRequest request = new TrackBatchRequest();
        request.setA("test-app");
        request.setU("user-123");
        request.setUp(Map.of("plan", "premium"));
        
        TrackBatchRequest.EventDTO event = new TrackBatchRequest.EventDTO();
        event.setT(5); // CUSTOM
        event.setId("purchase");
        event.setP(Map.of("product", "laptop"));
        request.setE(List.of(event));
        
        trackService.processBatchEvents(request);
        
        verify(eventRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void shouldHandleClickEventsWithDomPath() {
        TrackBatchRequest request = new TrackBatchRequest();
        request.setA("test-app");
        request.setU("user-123");
        
        TrackBatchRequest.EventDTO event = new TrackBatchRequest.EventDTO();
        event.setT(2); // CLICK
        event.setP(Map.of(
            "domPath", "html>body>div>button",
            "tagName", "BUTTON",
            "className", "btn-primary"
        ));
        request.setE(List.of(event));
        
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
