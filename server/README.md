# Track 后端服务（server/）

本服务为 Track 埋点分析平台的 Spring Boot 后端，采用 Java 17+，分层结构清晰，开箱即用，自动生成接口文档，TDD 首先、兼容云原生。

## 快速开始

```bash
# 安装依赖
./mvnw clean install

# 启动开发环境（默认8080端口）
./mvnw spring-boot:run

# 运行全部测试
./mvnw test
```

## 代码结构

- config/      —— 全局配置与拦截器
- controller/  —— 控制器，API入口
- service/     —— 业务核心
- repository/  —— 数据访问
- domain/      —— 领域模型
- dto/         —— 数据传输对象
- util/        —— 工具

入口类：io.github.sijiezhong.track.TrackApplication

## 自动文档
集成 springdoc-openapi，启动服务后自动生成 Swagger 文档。
访问：`/swagger-ui.html` 可查看全部API与Schema。

## TDD与测试
所有功能必须先编写单元/集成测试再开发实现，推荐 JUnit 5、Spring Boot Test、Testcontainers。主流程测试覆盖率80%+

---
作者：sijie

