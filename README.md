# Track 埋点分析平台（开源项目）

Track 是一套完全开源、可商用的全栈埋点分析解决方案，支持多端数据采集、弹性后端存储计算与专业可视化分析。系统包含三大独立模块：

- 前端埋点 SDK（TypeScript/JavaScript，框架无关，支持主流 web 应用采集上报）
- 后端服务（Java Spring Boot，PostgreSQL+Redis，全开源协议）
- 数据可视化前端（React + Ant Design + G2/G6，数据大屏&仪表盘）

## 项目亮点
- **企业级分层架构**，快速二次开发与集成
- **无依赖任何商业服务**，依赖全部为开源协议
- **数据自动批量上报/重试/匿名机制**，支持复杂埋点场景
- **API 支持 OpenAPI 自动文档与接口调试**
- **内置 TDD、CI/CD 最佳实践**，测试覆盖率红线，分支保护
- **Docker/K8s 一键部署，云原生可扩展**

## 总体架构
```
[采集SDK]  <---埋点--->  [后端 API服务]  <---SSE/接口--->  [可视化前端]
```

- 支持主流浏览器、SPA/MPA，采集/分批上报/匿名等机制
- 后端基于 Postgres/Redis/SpringBoot，分层结构&高性能
- 前端仪表盘支持实时监控、大屏场景

## 目录结构
```
/                 # 仓库根目录
├─ README.md      # 项目简介 (本文件)
├─ dev-guide.md   # 开发与贡献指南（TDD/接口/架构详述）
├─ LICENSE        # 开源协议（MIT/Apache-2.0）
├─ sdk/           # 前端埋点SDK项目
├─ server/        # 后端Spring Boot服务
└─ analytics-web/ # 前端可视化React项目
```
> 各子项目均自带独立 README/README.md 及构建说明，无需商业 key 或注册。

## 快速开始
详见各子项目 README。

## 开源与合规
本项目及所有依赖全部为 Apache/MIT/GPL 等主流开源协议（详见 LICENSE）。禁止引入任何收费或闭源依赖，代码可自由商用、二开。

---

如要参与贡献，请查阅 dev-guide.md 或提交 Issue/PR 与我们联系。
