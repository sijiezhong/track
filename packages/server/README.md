# Track Server

Track 埋点数据分析平台服务端实现

## 技术栈

- Spring Boot 3.2.x
- Java 17
- PostgreSQL 15
- Redis 7
- Maven
- JUnit 5 + Testcontainers

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 启动服务

1. 启动 PostgreSQL 和 Redis：
```bash
cd packages/server
docker-compose up -d
```

2. 运行应用：
```bash
mvn spring-boot:run
```

3. 访问 API 文档：
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=SessionServiceTest
```

## 项目结构

```
src/
├── main/
│   ├── java/com/track/
│   │   ├── Application.java          # 主启动类
│   │   ├── config/                   # 配置类
│   │   ├── controller/               # 控制器
│   │   ├── service/                  # 服务层
│   │   ├── repository/               # 数据访问层
│   │   ├── entity/                   # 实体类
│   │   └── dto/                      # 数据传输对象
│   └── resources/
│       ├── application.yml           # 配置文件
│       └── db/migration/             # Flyway 数据库迁移脚本
└── test/
    └── java/com/track/
        ├── config/                   # 配置测试
        ├── controller/               # 控制器测试
        ├── service/                  # 服务层测试
        └── integration/              # 集成测试
```

## API 接口

### Session 管理

- `POST /api/session` - 注册会话
- `POST /api/session/refresh` - 刷新会话
- `POST /api/session/destroy` - 销毁会话

### 事件采集

- `POST /api/ingest` - 批量事件采集

### 数据分析

- `GET /api/analytics/overview` - 概览 KPI
- `GET /api/analytics/pv-uv/series` - PV/UV 趋势
- 更多接口详见 API 文档

## 开发说明

### TDD 实践

本项目严格遵循 TDD（测试驱动开发）原则：
1. 先编写测试
2. 运行测试（应该失败）
3. 实现功能
4. 运行测试（应该通过）
5. 重构

### 测试策略

- **单元测试**：使用 Mockito 隔离依赖，验证业务逻辑
- **集成测试**：使用 Testcontainers 启动真实的数据库和 Redis
- **测试重点**：验证业务场景和数据正确性，不只是覆盖率

## 环境配置

### 开发环境

```bash
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/track
  data:
    redis:
      host: localhost
      port: 6379
```

### 测试环境

测试使用 Testcontainers 自动启动 PostgreSQL 和 Redis 容器，无需手动配置。

## 数据库迁移

使用 Flyway 管理数据库迁移脚本，位于 `src/main/resources/db/migration/`。

迁移脚本命名规则：`V{version}__{description}.sql`

## 许可证

详见项目根目录 LICENSE 文件

