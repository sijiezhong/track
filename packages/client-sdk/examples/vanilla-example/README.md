# Vanilla JS 示例

这是一个使用原生 JavaScript 和 Script 标签引入 SDK 的完整示例项目。

## 📋 目录结构

```
vanilla-js/
├── index.html          # 示例页面
└── README.md          # 说明文档
```

## ✅ 验证构建

构建完成后，可以运行检查脚本验证构建文件：

```bash
node check-build.js
```

### 1. 构建 SDK

首先需要构建 SDK：

```bash
cd packages/client-sdk
npm install
npm run build
```

构建完成后，会在 `dist` 目录下生成以下文件：
- `index.umd.js` - UMD 格式，可用于浏览器直接引入
- `index.js` - ES Module 格式
- `index.cjs` - CommonJS 格式

### 2. 运行示例

#### 方式一：直接打开 HTML 文件

直接在浏览器中打开 `index.html` 文件：

```bash
# macOS
open index.html

# Linux
xdg-open index.html

# Windows
start index.html
```

#### 方式二：使用本地服务器（推荐）

使用 Python 启动一个简单的 HTTP 服务器：

```bash
# Python 3
python3 -m http.server 3000

# Python 2
python -m SimpleHTTPServer 3000
```

然后访问 `http://localhost:3000`

或者使用 Node.js 的 `http-server`：

```bash
npx http-server -p 3000
```

### 3. 配置服务端

确保你的服务端正在运行，默认地址为 `http://localhost:8080`。

如果需要修改服务端地址，可以在示例页面的配置区域修改 `endpoint` 字段。

## ✨ 功能演示

此示例演示了以下功能：

### 核心功能

- ✅ **SDK 初始化** - 配置用户信息和追踪参数
- ✅ **启动/停止追踪** - 控制 SDK 的运行状态
- ✅ **自定义事件上报** - 手动上报业务事件

### 自动采集功能

- ✅ **PV (页面浏览)** - 自动采集页面访问事件
- ✅ **点击事件** - 自动采集用户点击行为（支持多维度过滤）
- ✅ **页面停留时长** - 自动采集页面停留时间
- ✅ **性能指标** - 自动采集页面加载性能数据
- ✅ **错误监控** - 自动采集 JavaScript 错误和 Promise 拒绝

### 高级功能

- ✅ **SPA 路由监听** - 监听 `pushState`、`replaceState` 和 `hashchange`
- ✅ **点击追踪配置** - 演示点击事件的过滤规则
- ✅ **实时日志** - 查看 SDK 的运行状态和事件上报情况

## 🎯 使用示例

### 基本使用

```javascript
// 1. SDK 加载后，window.Track 是一个对象，包含：
//    - Track: Track 类
//    - track: 单例实例（推荐使用）
//    - default: 默认导出（也是单例实例）

// 获取单例实例
const track = window.Track.track || window.Track.default;

// 2. 初始化 SDK
await track.init({
  appId: 'your-app-id',
  userId: 'user-123',
  userProps: {
    plan: 'premium',
    version: '1.0.0'
  }
}, {
  endpoint: 'http://localhost:8080',
  autoTrack: true,
  performance: true,
  errorTrack: true
});

// 3. 启动追踪
track.start();

// 4. 上报自定义事件
track.track('button_click', {
  buttonId: 'submit-btn',
  category: 'action'
});

// 5. 停止追踪
await track.stop();
```

**注意**：示例页面会自动处理 SDK 实例的获取，使用 `window.TrackSDK` 来访问单例实例。

### 配置选项

#### UserConfig（用户配置）

```javascript
{
  appId: string;           // 应用 ID（必填）
  userId: string;          // 用户 ID（必填）
  userProps?: {           // 用户属性（可选）
    [key: string]: any;
  };
}
```

#### TrackConfig（追踪配置）

```javascript
{
  endpoint: string;                    // 服务端地址（必填）
  autoTrack?: boolean;                 // 是否启用自动采集（默认：true）
  performance?: boolean;               // 是否采集性能数据（默认：true）
  errorTrack?: boolean;                // 是否启用错误监控（默认：true）
  sessionTTL?: number;                 // Session 有效期（分钟，默认：1440）
  debug?: boolean;                     // 是否启用调试模式（默认：false）
  clickTrack?: ClickTrackConfig | false; // 点击追踪配置（默认：启用）
}
```

## 🔍 调试

### 启用调试模式

在初始化时设置 `debug: true`：

```javascript
await window.Track.init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'http://localhost:8080',
  debug: true  // 启用调试模式
});
```

调试模式下，SDK 会在控制台输出详细的日志信息。

### 查看上报数据

打开浏览器开发者工具（F12），查看：

1. **Network 标签** - 查看发送到服务端的请求
2. **Console 标签** - 查看 SDK 的日志输出
3. **Application 标签** - 查看 LocalStorage 中存储的数据

## 📝 注意事项

1. **CORS 问题**：如果服务端和前端不在同一域名，需要配置 CORS
2. **HTTPS**：在生产环境中，建议使用 HTTPS
3. **数据隐私**：确保遵守相关数据隐私法规
4. **性能影响**：SDK 已经过优化，对页面性能影响很小

## 🐛 常见问题

### Q: SDK 未加载怎么办？

A: 确保已运行 `npm run build` 构建 SDK，并且 `index.html` 中的脚本路径正确。

### Q: 事件没有上报到服务端？

A: 检查以下几点：
1. 服务端是否正在运行
2. `endpoint` 配置是否正确
3. 是否已调用 `start()` 方法
4. 查看浏览器控制台是否有错误信息

### Q: 如何查看采集的数据？

A: 打开浏览器开发者工具，查看 Network 标签中的请求，或者在控制台查看日志输出。

## 📚 更多资源

- [SDK API 文档](../../README.md)
- [点击追踪策略文档](../../../docs/click-tracking-strategy.md)
- [完整文档](../../README.md)

## 🤝 贡献

如果发现示例项目有问题或需要改进，欢迎提交 Issue 或 Pull Request。

