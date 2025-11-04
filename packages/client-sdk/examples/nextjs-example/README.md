# Track SDK - Next.js 示例项目

本示例演示在 Next.js (App Router) 中集成并验证 `@track/sdk`。页面提供了手动初始化、启动/停止、测试上报与日志面板，方便联调。

## 目录结构

- `src/app/page.tsx`: 示例页面（客户端组件），手动触发 SDK 初始化与测试
- `src/app/layout.tsx`: 基础布局与全局样式引入
- `public/`: 仅保留必要资源（已清理默认 SVG）

## 前置条件

- Node.js ≥ 18
- pnpm ≥ 8

## 安装与运行

1) 构建 SDK（确保示例通过 `link:../..` 能解析到构建产物）：

```bash
cd packages/client-sdk
pnpm build
```

2) 安装并启动 Next.js 示例：

```bash
cd packages/client-sdk/examples/nextjs-example
pnpm install
pnpm dev
```

启动后访问终端输出的本地地址（默认 `http://localhost:3000/`）。

可选：配置默认 Endpoint（推荐在示例目录中设置）：

```bash
echo "NEXT_PUBLIC_TRACK_ENDPOINT=http://localhost:8080" > .env.local
```

## 页面使用说明

1) 在页面顶部“⚙️ SDK 配置”区域填写：
   - `Endpoint`（服务端地址，如 `http://localhost:8080` 或自有域名）
   - `App ID`、`User ID`
   - `Session TTL`（分钟）
2) 点击“初始化 SDK”执行 `track.init(...)`，成功后点击“启动追踪”执行 `track.start()`。
3) 使用测试区按钮验证：
   - 手动触发 PV（通过 `history.pushState`）
   - 触发错误与 Promise 错误
   - 上报自定义事件与批量事件
4) 底部“📋 操作日志”显示关键步骤与结果。

## 与 SDK 的联动

示例使用 `@track/sdk` 单例并启用：`autoTrack`、`clickTrack`、`performance`、`errorTrack`。

片段（详见 `src/app/page.tsx` 的 `onInit`）：

```ts
await track.init(
  { appId, userId, userProps: { source: "nextjs-example" } },
  { endpoint, autoTrack: true, performance: true, errorTrack: true, sessionTTL, clickTrack: { enabled: true } }
);
```

## 后端接口要求

SDK 在初始化与运行时会调用（均为 `POST`）：

- `/api/session`（初始化 Session，设置 Cookie）
- `/api/session/refresh`（刷新 Session）
- `/api/session/destroy`（销毁 Session）

若前端携带 Cookie（默认 `credentials: 'include'`），请服务端开启 CORS：

- `Access-Control-Allow-Origin: http://localhost:3000`（或你的前端来源，不能为 `*`）
- `Access-Control-Allow-Credentials: true`
- 正确处理 `OPTIONS` 预检

## 常见问题（FAQ）

- Module not found: Can't resolve `@track/sdk`
  - 先在 SDK 包目录执行 `pnpm build`
  - 确认示例 `package.json` 使用 `"@track/sdk": "link:../.."`
  - 在示例目录 `pnpm install` 后重启 `pnpm dev`

- 初始化报错 `TypeError: Failed to fetch`
  - 说明：请求 `POST {endpoint}/api/session` 失败
  - 排查：后端未启动或地址错误、CORS 未放行且携带 Cookie、协议/域名不一致、接口未实现
  - 自查（替换实际地址）：
    ```bash
    curl -i -X POST http://localhost:8080/api/session \
      -H "Content-Type: application/json" \
      -d '{"appId":"a","userId":"u"}'
    ```

## 开发提示

- 本示例不会在应用启动时自动初始化 SDK，需通过页面按钮手动执行
- 可通过 `.env.local` 配置 `NEXT_PUBLIC_TRACK_ENDPOINT` 作为默认后端地址
- 使用浏览器 Network/Console 面板查看请求与日志
