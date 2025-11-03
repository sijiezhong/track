# 埋点分析平台开发文档

本项目是一套开源、可商用的全栈埋点分析平台，由前端埋点 SDK、后端服务和数据可视化分析台三大模块组成，支持多系统集成、灵活采集及深入分析。

---

## 项目架构概览
- 前端 SDK 使用 TypeScript 编写，兼容所有主流前端框架。
- 后端基于 Java 17+ 与 Spring Boot，采用 PostgreSQL 作为主库，Redis 提升高并发能力。
- 可视化分析采用 React + Ant Design + G2/G6，专注数据大屏与多维分析。
- 整体支持 Docker、Kubernetes 部署。

---

## 前端埋点 SDK 模块
- 设计为框架无关，可原生植入 JS/TS 工程，也能适配主流 SPA/MPA。
- 提供用户身份（user_id、user_name、project_id）注册与清除功能，自动退回匿名模式。
- 上报协议采用 1x1 GIF 图片请求，天然规避跨域问题。
- 支持批量分片上报（批量阈值达标即刻上报，未达的在页面卸载时兜底），保证数据可靠与顺序一致。
- 埋点控制模式灵活：既可自动上报，也支持手动启停和动态切换。
- 域名与报送接口可参数配置，易于多环境切换。
- 内建重试机制（默认最多 5 次，指数递增延迟，maxRetries/retryDelay/retryBackoff 可配），异常可按错误类型分策略处理。
- 报文数据标准化，包含时间戳、事件类型、页面 URL、UA、批次信息等。
- API 支持自定义事件（trackEvent）、开发环境调试模式输出、语义化版本管理。
- 浏览器兼容性涵盖 IE11+ 和所有现代浏览器。

**典型 API 调用方式**：
```javascript
const config = {
  autoStart: true, // 自动或手动启动
  endpoint: 'https://api.example.com',
  projectId: 'your-project-id',
  maxRetries: 5,
  retryDelay: 1000,
  batchSize: 10,
  // ... 其他定制项
};
sdk.startTracking();
sdk.stopTracking();
sdk.setUser(userInfo);
sdk.clearUser();
sdk.trackEvent(eventData);
```

---

## 后端服务核心设计
- 用 Java 17+/Spring Boot 构建，工程结构分 controller、service、repository、domain、dto、config、util 等典型包。
- 数据库为 PostgreSQL，核心表结构包括 users（用户）、applications（应用）、events（事件数据）、sessions（会话）。数据默认全量保留，支持匿名数据自动填充与后续合并实名。
- Redis 提供高速缓存、会话管理等能力，高并发保障。
- 数据上报支持图片接口和标准 RESTful API，兼容浏览器埋点与其它来源上报。
- 认证采用 JWT 等主流安全机制，API 权限与 HTTPS 强制要求均为默认安全基线。
- 支持多维数据查询、用户和应用管理、以及基于 SSE（Server-Sent Events）的实时推送功能。
- 项目集成 springdoc-openapi，实现 Controller 自动生成 OpenAPI3/Swagger 文档，访问 `/swagger-ui.html` 即可在线调试全部接口。
- 强调连接池、索引等性能优化、完备请求及异常日志。
- 所有依赖库均为 Apache/MIT/GPL 社区协议，无闭源无收费无云端绑定。

**springdoc-openapi 引入示例**：
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

---

## 数据可视化分析前端
- 使用 React + Ant Design 实现高复用、响应式前端架构。
- 数据可视化基于 G2/G6，支持折线、柱状、饼图、热力、关系、地理空间等多种高级图形能力。
- 核心功能包括用户认证、指标仪表盘、趋势分析、实时监控（SSE 数据接入)、管理后台（用户、权限、应用）和大屏展示。
- 点对点支持多维度筛选、时间窗口对比、行为与会话漏斗分析。
- 数据可导出（CSV/PDF），页面自适应多终端。

---

## 数据流与机制说明
- 埋点数据流：前端 SDK → 1x1 GIF 请求/REST API → 后端接口 → PostgreSQL 存储。
- 实时数据流：数据库变更 → 后端服务监听 → SSE 向前端推送 → 前端仪表盘实时刷新。
- 分片与兜底：达到 batchSize 即时上报，未达时 unload 事件汇总后兜底一次发完。
- 重试机制：支持网络异常/服务端失败多策略自动补偿。
- 匿名模式：clearUser 无用户信息时自动进入，后端仍给匿名会话；用户后续登录后进行数据合并。

---

## 工程结构与目录规范
- 根目录下：README.md（简介），DEVELOPMENT.md（本开发说明），LICENSE（MIT/Apache-2.0），三大子项目。
- `sdk/`：TypeScript 前端埋点包。
- `server/`：Spring Boot 全开源后端，见 pom.xml 及典型 Java 包、分层目录。
- `analytics-web/`：React+AntD 可视化平台，主流 src 公共结构。

```plaintext
/
├─ README.md	        # 项目简介
├─ DEVELOPMENT.md	# 技术与开发规格文档
├─ LICENSE	        # 开源协议文件
├─ sdk/		        # 前端埋点 SDK
├─ server/	        # 核心后端服务（分层 + 测试 + OpenAPI）
└─ analytics-web/	# 可视化前端项目
```
- 各子项目均有单独构建说明和依赖清单，保证独立可维护

---

## TDD 规范与开源合规
- 项目后端全员遵循 TDD（测试驱动开发），任何功能先写或补充测试再实现。
- 推荐/要求测试工具全部开源：JUnit 5、Mockito、AssertJ（单元），SpringBoot Test、Testcontainers（集成，含数据库、Redis模拟）、Rest Assured（API自动化）。
- Pull Request 合入需覆盖率红线（推荐 80%+）、CI 支持、附带对应测试。
- 推荐测试目录：server/src/test/java/com/example/track/ 下分模块放置，application-test.yml 测试专用配置。

```plaintext
server/
  src/
    test/
      java/com/example/track/
        controller/
        service/
        repository/
      resources/
        application-test.yml
```

- 全项目禁止引入任何商业/收费/注册型外部依赖。所有工程产物与依赖均可自由分发、二次开发和商用，协议详见 LICENSE 与 server/pom.xml。

---
如有开发/架构/贡献问题，请参考本文件或于仓库 Issue 区交流。

---

## 推荐目录结构与技术细节

建议实际工程分层如下，便于团队协作、二次开发与自动化测试/部署：

```plaintext
/track
├── README.md              # 项目简介
├── DEVELOPMENT.md         # 技术和开发详细规范
├── LICENSE                # 开源协议
├── sdk/                   # 前端埋点 SDK
│   ├── package.json
│   ├── tsconfig.json
│   ├── README.md
│   └── src/
│       ├── index.ts
│       ├── core/
│       ├── api/
│       ├── utils/
│       └── types/
├── server/                # Spring Boot 后端服务
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/track/
│   │   │   │       ├── TrackApplication.java
│   │   │   │       ├── config/
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── domain/
│   │   │   │       ├── dto/
│   │   │   │       └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── schema.sql
│   │   │       └── data.sql
│   │   └── test/
│   │       └── java/com/example/track/
│   │           ├── controller/
│   │           ├── service/
│   │           ├── repository/
│   │           └── domain/
│   │       └── resources/
│   │           └── application-test.yml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── README.md
├── analytics-web/         # 前端可视化平台 React 项目
│   ├── package.json
│   ├── tsconfig.json
│   ├── README.md
│   └── src/
│       ├── App.tsx
│       ├── pages/
│       ├── components/
│       ├── hooks/
│       ├── services/
│       └── assets/
└── .gitignore
```

### sdk/ 前端埋点 SDK
- TypeScript 构建，组织为 core（采集主流程）、api（上报与重试）、utils、types 分模块，确保每层职责单一、易测和易扩展。
- 输出多格式包：esm、cjs 和 UMD，可用于 npm 依赖或 script 嵌入。
- Jest/ts-jest 支持单元测试，建议 80%+ 覆盖率。
- 配置严格类型约束，接口参数与事件数据都有明确类型定义。

### server/ Spring Boot 后端
- 完全基于开源依赖，Java 17+，分层组织与 DDD 最佳实践。
- 主业务拆分为 controller（路由）、service（逻辑）、repository（数据访问）、domain（实体）、dto（传输对象）、config（配置）、util（工具类）。
- PostgreSQL 与 Redis 双引擎，数据层抽象，支持多环境 yml 配置。
- 接口全部自动生成 Swagger OpenAPI，支持在线调试。
- 集成 JUnit5/Spring Boot Test/Testcontainers，覆盖率门槛 80%。
- Dockerfile/docker-compose 支持全链路容器/云原生部署。

### analytics-web/ 可视化分析前端
- 基于 React + TypeScript，全量使用 hooks，目录清晰拆分页面、组件、hooks、服务、静态资源。
- G2/G6 构建趋势、漏斗、关系等各类图表，页面数据源通过 hooks 动态加载。
- 支持 SSE 推送和大屏自适应，Redux 或 Recoil 全局状态管理。
- Ant Design 组件库适配后台和大屏主题切换，国际化与权限控制均有预留。

