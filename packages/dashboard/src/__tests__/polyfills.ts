// Polyfills for Node.js environment
// 这需要在所有其他导入之前执行
// 修复 MSW 依赖（whatwg-url, webidl-conversions）在 Node.js 环境中的问题

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
// 这些应该在 Node.js 中默认可用，但为了安全起见，我们确保它们存在
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
