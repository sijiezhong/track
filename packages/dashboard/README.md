# Track Dashboard

基于 Vite + React + TypeScript + Tailwind + shadcn-ui，图表采用 @antv/g2，路径图采用 @antv/g6。开发阶段使用 vite-plugin-mock + mockjs 提供接口数据。

## 开发

```bash
pnpm --filter @track/dashboard install
pnpm --filter @track/dashboard dev
```

## 环境变量
- VITE_API_BASE_URL：服务端基础地址（留空时使用相对路径，便于本地 mock 拦截）
- VITE_ENABLE_MOCK：是否启用本地 mock（开发建议为 true，生产必须为 false）

在 Mac/Linux 可临时注入：
```bash
VITE_API_BASE_URL="" VITE_ENABLE_MOCK=true pnpm --filter @track/dashboard dev
```

示例 .env 文件（若仓库忽略了 .env.example，可自行创建）：
```env
VITE_API_BASE_URL=
VITE_ENABLE_MOCK=true
```

## 路由
- /overview
- /analytics/trends
- /analytics/pages
- /analytics/custom
- /analytics/performance
- /analytics/errors
- /user/behavior
- /events

## Mock 一键移除
- 删除 `packages/dashboard/mock/` 目录
- 删除 `vite.config.ts` 中 `vite-plugin-mock` 插件配置

## shadcn-ui 组件库
- 本项目已配置 Tailwind；如需接入 shadcn-ui，请按官网 CLI 初始化到本包目录：
```bash
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card input select table tabs toast
```
- 也可直接使用当前的 Tailwind 组件样式。


