# Track SDK - Vue 3 示例项目

该示例展示如何在 Vue 3 + Vite + TypeScript 项目中集成并验证 `@track/sdk`。页面提供了可视化的参数填写、初始化、启动/停止、测试上报与日志面板，便于联调与排查。

## 目录结构

- `src/App.vue`: 示例 UI 与事件绑定，手动触发 SDK 初始化与测试
- `src/main.ts`: 仅挂载应用（不在应用启动时自动 init）
- `vite.config.ts`: Vite 基础配置

## 前置条件

- Node.js >= 18
- pnpm >= 8

## 安装与运行

1) 在 SDK 包目录构建 SDK（确保可被示例以 `link:../..` 解析）：

```bash
cd packages/client-sdk
pnpm build
```

2) 安装并启动 Vue 示例：

```bash
cd packages/client-sdk/examples/vue3-example
pnpm install
pnpm dev
```

启动后访问终端输出的本地地址（默认 `http://localhost:5173/`）。

## 页面使用说明

1) 打开页面后，首先在 “⚙️ SDK 配置” 区域填写：
   - `Endpoint`（服务端地址，例如 `http://localhost:8080`）
   - `App ID`、`User ID`
   - `Session TTL`（分钟）
2) 点击 “初始化 SDK” 执行 `track.init(...)`，成功后再点击 “启动追踪” 执行 `track.start()`。
3) “📊 自动采集与测试” 与 “🎯 自定义事件上报” 区域可以验证：
   - 手动触发 PV（通过 `history.pushState`）
   - 触发错误与 Promise 错误
   - 上报自定义事件与批量事件
4) “📋 操作日志” 面板会实时输出关键操作与结果。

提示：你也可以通过 `.env.local` 设置默认后端地址，例如：

```bash
echo "VITE_TRACK_ENDPOINT=http://localhost:8080" > .env.local
```

## 与 SDK 的联动

示例使用 `@track/sdk` 的单例实例，并启用如下特性：

- `autoTrack`：自动采集 PV、点击等
- `clickTrack`：点击追踪（默认开启）
- `performance`：性能监控
- `errorTrack`：错误监控

这些开关在 `src/App.vue` 内部 `onInit` 时通过第二个参数传入：

```ts
await track.init({ appId, userId, userProps }, {
  endpoint,
  autoTrack: true,
  performance: true,
  errorTrack: true,
  sessionTTL: 1440,
  clickTrack: { enabled: true },
})
```

## 后端接口要求

SDK 在初始化与运行过程中会请求以下接口（均为 `POST`）：

- `/api/session`（初始化 Session，设置 Cookie）
- `/api/session/refresh`（刷新 Session）
- `/api/session/destroy`（销毁 Session）

如果前端启用 `credentials: 'include'`（SDK 默认如此），请在服务端开启 CORS 并返回：

- `Access-Control-Allow-Origin: http://localhost:5173`（或你的实际前端来源，不能是 `*`）
- `Access-Control-Allow-Credentials: true`
- 正确处理 `OPTIONS` 预检请求

## 常见问题（FAQ）

- [vue3-example] Track init failed: TypeError: Failed to fetch
  - 说明：浏览器请求 `POST {endpoint}/api/session` 失败。
  - 排查：
    - 后端未启动或地址写错；
    - CORS 未正确设置，且我们携带了 Cookie；
    - 协议/域名不一致（https 页面请求 http，或 `localhost` 与 `127.0.0.1` 混用导致跨站 Cookie）；
    - 服务端未实现上述接口。
  - 自查命令（替换实际地址）：
    ```bash
    curl -i -X POST http://localhost:8080/api/session \
      -H "Content-Type: application/json" \
      -d '{"appId":"a","userId":"u"}'
    ```

- 运行时找不到 `@track/sdk`：
  - 先在 SDK 包目录执行 `pnpm build`；
  - 确认本示例 `package.json` 中依赖为 `"@track/sdk": "link:../.."`；
  - 在示例目录重新安装依赖 `pnpm install` 并重启 `pnpm dev`。

## 开发提示

- 本示例不会在应用启动时自动初始化 SDK，需通过页面按钮手动执行，便于联调。
- 如需修改默认 Endpoint，可在 `.env.local` 配置 `VITE_TRACK_ENDPOINT`。
- 可在浏览器 Network 面板与 Console 查看请求与日志详情。

---
如有问题，欢迎在页面日志面板或浏览器控制台查看报错，并对照本 README 的 FAQ 进行排查。


