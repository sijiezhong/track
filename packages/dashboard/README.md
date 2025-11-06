# Track Dashboard

基于 Vite + React + TypeScript + Tailwind + shadcn-ui，图表采用 @antv/g2，路径图采用 @antv/g6。现已切换到真实后端接口，不再使用 mock 数据。

## 开发

```bash
pnpm --filter @track/dashboard install
pnpm --filter @track/dashboard dev
```

## 环境变量

### 方式一：使用 Mock 数据（开发推荐）

使用本地 mock 数据，无需连接后端服务。适合快速开发和测试。

创建 `.env.local` 文件（不会提交到 git）：
```env
VITE_ENABLE_MOCK=true
```

### 方式二：使用 Proxy 代理

开发环境使用 proxy 代理，需要配置 `VITE_PROXY_TARGET`。所有 `/api` 请求会自动代理到配置的目标地址。

创建 `.env.local` 文件：
```env
VITE_ENABLE_MOCK=false
VITE_PROXY_TARGET=https://your-api-domain.com
```

### 方式三：直接配置 API 地址

如果设置了 `VITE_API_BASE_URL`，则不会使用 proxy 和 mock，直接使用配置的地址：
```env
VITE_ENABLE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080
```

**优先级说明**：
1. 如果 `VITE_ENABLE_MOCK=true`：使用 mock 数据，忽略 proxy 和 API 地址配置
2. 如果设置了 `VITE_API_BASE_URL`：直接使用该地址，不使用 proxy 和 mock
3. 如果设置了 `VITE_PROXY_TARGET` 且未设置 `VITE_API_BASE_URL`：使用 proxy 代理

**注意**：
- `.env.local` 文件不会被提交到 git（已在 .gitignore 中）
- Mock 功能仅在开发环境生效
- 参考 `.env.local.example` 文件创建本地配置

## 路由
- /overview
- /analytics/trends
- /analytics/pages
- /analytics/custom
- /analytics/performance
- /analytics/errors
- /user/behavior
- /events

## 测试
- 使用 MSW (Mock Service Worker) 进行单元测试
- Mock handlers 位于 `src/__tests__/mocks/handlers.ts`
- 运行测试：`pnpm test`

## Mock 数据
- Mock 数据位于 `packages/dashboard/mock/` 目录
- 通过 `VITE_ENABLE_MOCK=true` 环境变量启用（仅开发环境）
- 测试环境使用 MSW 模拟后端接口（`src/__tests__/mocks/handlers.ts`）

## shadcn-ui 组件库
- 本项目已配置 Tailwind；如需接入 shadcn-ui，请按官网 CLI 初始化到本包目录：
```bash
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card input select table tabs toast
```
- 也可直接使用当前的 Tailwind 组件样式。


