# 业务需求（标准化条款）

## 1. 埋点SDK
- SDK 初始化仅需 project_id、endpoint。
- 支持页面访问、点击、性能、错误等自动采集和手动 trackEvent。
- 前端只上报 event_type: string 和 event_content: object。
- timestamp, user_id, session_id, anonymous_id, channel, device, browser, ip, ua, referrer, project_id 由服务端补全。
- 批量上报：本地队列，超时/batchSize/beforeunload 时发送。
- setUser/clearUser 改变实名/匿名模式，合并由服务端实施。
- 支持多框架和多实例，project_id 区分环境。

## 2. 数据上报与服务端处理
- 服务端接口 /api/events/collect 接收 GET/POST。
- 标准字段：event_type, project_id（必填），event_content（可选）。
- 服务端自动补全所有能推导字段。
- 存储流程：输入→字段补齐→校验去重→会话归并→写 events 表→推送实时流。

## 3. 用户与会话模型
- events 表结构：event_id, event_type, project_id, user_id, session_id, anonymous_id, event_content, timestamp, channel, device, browser, os, ip, ua, referrer, tenant_id, extra。
- setUser/clearUser 只负责切换身份，所有合并归因/关联后端完成。
- 确保物理/逻辑多租户隔离（project_id/tenant_id）。

## 4. 分析与数据能力
- 分析：漏斗、路径、留存、分群、热点、多租户逻辑分析。
- 维度：channel、device、app_version、user_id/anonymous_id、page_url、event_content.{key}。
- 支持API分页、任意字段导出（CSV/JSON/Parquet）。

## 5. AI及自动化接口
- 所有字段、接口、模型均结构化命名。
- 数据聚合、归因、聚类全部服务端处理。
- 提供 OpenAPI 及 webhook，AI 服务可通过过滤与组合直接查询、分析事件链。

## 6. 权限/运维/安全
- 角色：admin/analyst/developer/readonly，分字段/接口授权。
- 操作日志：所有变更操作均记录操作者、内容、时间。
- 配置全部外部化：env/yml。
- 健康监控、报警、备份、快照支持。

## 7. 用例举例
- 电商转化漏斗：event_type=['page_view','add_to_cart','purchase']，查询页面点击到购买路径。
- 金融开户流程：event_type=['register','upload_id','auth_success','contract']。
- 内容社区：event_type=['publish_post','comment','login','like']，查询匿名/实名转化。
- SaaS多产品线：不同project_id，租户逻辑隔离。

