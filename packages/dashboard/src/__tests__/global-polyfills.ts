// 全局 polyfills - 必须在所有其他代码之前执行
// 这个文件被 vitest.config.ts 中的 globalSetup 使用

// 确保 globalThis.global 存在（某些包依赖这个）
if (typeof globalThis !== "undefined") {
  // @ts-ignore
  if (!globalThis.global) {
    // @ts-ignore
    globalThis.global = globalThis;
  }

  // 确保 process 对象存在
  if (typeof process === "undefined") {
    // @ts-ignore
    globalThis.process = {
      env: {},
      version: "",
      versions: {},
      platform: "node",
      nextTick: (fn: Function) => setTimeout(fn, 0),
    };
  }
}

// 确保在 Node.js 环境中 Map 和 WeakMap 等内置对象可用
if (typeof Map === "undefined") {
  // @ts-ignore
  globalThis.Map = Map;
}
if (typeof WeakMap === "undefined") {
  // @ts-ignore
  globalThis.WeakMap = WeakMap;
}
if (typeof Set === "undefined") {
  // @ts-ignore
  globalThis.Set = Set;
}
