# Analytics Web - Track 埋点分析平台前端

基于 React 18 + TypeScript + Tailwind CSS + shadcn/ui 开发的数据分析可视化平台。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite 5
- **UI 组件**: shadcn/ui (基于 Radix UI)
- **样式**: Tailwind CSS 3.x
- **图表**: @antv/g2 5.x, @antv/g6 5.x
- **路由**: React Router 6
- **HTTP**: Axios
- **包管理**: pnpm

## 快速开始

### 安装依赖

```bash
pnpm install
```

### 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env.local

# 编辑 .env.local，配置后端地址
# VITE_API_TARGET=http://localhost:8080
```

### 启动开发服务器

```bash
pnpm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
pnpm run build
```

### 预览生产版本

```bash
pnpm run preview
```

## 功能特性

### 权限管理
支持四种角色：
- **ADMIN** (管理员): 所有功能权限
- **ANALYST** (分析师): 数据分析、查看权限
- **DEVELOPER** (开发者): 应用配置、事件查询
- **READONLY** (只读): 仅查看数据

### 核心功能

#### 1. 数据大屏（首页）
- 实时事件流展示（SSE）
- 6 个分析模块卡片
- 顶部统计数据
- 点击卡片跳转详细分析页

#### 2. 数据分析
- 趋势分析：查看事件趋势变化
- 漏斗分析：分析用户转化流程
- 留存分析：查看用户留存情况
- 路径分析：分析用户行为路径
- 分群分析：按维度分组统计
- 热点图：查看事件热点分布

#### 3. 管理后台
- 用户管理
- 应用管理
- Webhook 管理
- 操作日志

## 开发说明

### 项目结构

```
src/
├── components/        # 组件
│   ├── ui/           # shadcn/ui 组件
│   ├── layout/       # 布局组件
│   ├── auth/         # 认证组件
│   ├── charts/       # 图表组件
│   └── common/       # 通用组件
├── pages/            # 页面
│   ├── analytics/    # 分析页面
│   └── admin/        # 管理页面
├── contexts/         # React Context
├── hooks/            # 自定义 Hooks
├── services/         # API 服务
├── lib/              # 工具函数
├── types/            # 类型定义
├── constants/        # 常量
├── router/           # 路由配置
└── config/           # 应用配置
```

### 代理配置

开发环境使用 Vite 代理转发请求到后端，避免 CORS 问题：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: env.VITE_API_TARGET || 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

### 环境变量

- `.env.local` - 本地开发配置（不提交）
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置
- `.env.example` - 配置模板

## 后端接口依赖

需要后端提供以下接口：

### 认证接口
- `POST /api/v1/auth/login` - 用户登录（待实现）

### 分析接口（需要认证）
- `GET /api/v1/events/trend` - 趋势分析
- `GET /api/v1/events/funnel` - 漏斗分析
- `GET /api/v1/events/retention` - 留存分析
- `GET /api/v1/events/path` - 路径分析
- `GET /api/v1/events/segmentation` - 分群分析
- `GET /api/v1/events/heatmap` - 热点图
- `GET /api/v1/events/stream` - SSE 事件流

### 管理接口（需要 ADMIN 权限）
- `GET/POST /api/v1/admin/users` - 用户管理
- `GET/POST /api/v1/admin/apps` - 应用管理

## 部署

### Docker 部署

```bash
# 构建镜像
docker build -t track-analytics-web .

# 运行容器
docker run -p 3000:80 track-analytics-web
```

### Nginx 部署

构建后将 `dist` 目录部署到 Nginx，配置反向代理到后端 API。

## License

MIT