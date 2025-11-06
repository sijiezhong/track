"use client";
import { useCallback, useMemo, useRef, useState } from "react";
import track from "@track/sdk";

export default function Home() {
  const [endpoint, setEndpoint] = useState<string>(
    typeof window !== "undefined" ? window.location.origin : "",
  );
  const [appId, setAppId] = useState("nextjs-example-app-id");
  const [appName, setAppName] = useState("Next.js Example");
  const [userId, setUserId] = useState("user-123");
  const [sessionTTL, setSessionTTL] = useState<number>(1440);

  const [inited, setInited] = useState(false);
  const [started, setStarted] = useState(false);

  const [status, setStatus] = useState<{
    text: string;
    type: "info" | "success" | "error";
  }>({
    text: "等待初始化 SDK...",
    type: "info",
  });

  const logRef = useRef<HTMLDivElement | null>(null);

  const log = useCallback(
    (message: string, type: "info" | "success" | "error" = "info") => {
      const time = new Date().toLocaleTimeString();
      if (logRef.current) {
        const entry = document.createElement("div");
        entry.className = `log-entry ${type}`;
        entry.textContent = `[${time}] ${message}`;
        logRef.current.appendChild(entry);
        logRef.current.scrollTop = logRef.current.scrollHeight;
      }
      if (type !== "info")
        (console as any)[type === "error" ? "error" : "log"](message);
    },
    [],
  );

  const clearLog = useCallback(() => {
    if (logRef.current) logRef.current.innerHTML = "";
  }, []);

  const onInit = useCallback(async () => {
    try {
      if (!endpoint || !appId || !userId) {
        setStatus({ text: "❌ 请填写所有必填字段", type: "error" });
        return;
      }
      setStatus({ text: "⏳ 正在初始化...", type: "info" });
      log("正在初始化 SDK...", "info");
      await track.init(
        {
          appId,
          appName,
          userId,
          userProps: {
            plan: "premium",
            version: "1.0.0",
            source: "nextjs-example",
          },
        },
        {
          endpoint,
          autoTrack: true,
          performance: true,
          errorTrack: true,
          sessionTTL,
          clickTrack: { enabled: true },
        },
      );
      setInited(true);
      setStatus({
        text: '✅ SDK 初始化成功，点击"启动追踪"开始使用',
        type: "success",
      });
      log("SDK 初始化成功", "success");
    } catch (e: any) {
      setStatus({ text: `❌ 初始化失败: ${e?.message || e}`, type: "error" });
      log(`初始化失败: ${e?.message || e}`, "error");
    }
  }, [endpoint, appId, userId, sessionTTL, log]);

  const onStart = useCallback(() => {
    try {
      track.start();
      setStarted(true);
      setStatus({ text: "✅ SDK 已启动，正在采集数据...", type: "success" });
      log("SDK 已启动，开始自动采集", "success");
    } catch (e: any) {
      setStatus({ text: `❌ 启动失败: ${e?.message || e}`, type: "error" });
      log(`启动失败: ${e?.message || e}`, "error");
    }
  }, [log]);

  const onStop = useCallback(async () => {
    try {
      await track.stop();
      setStarted(false);
      setInited(false);
      setStatus({ text: "⏸️ SDK 已停止", type: "info" });
      log("SDK 已停止", "info");
    } catch (e: any) {
      setStatus({ text: `❌ 停止失败: ${e?.message || e}`, type: "error" });
      log(`停止失败: ${e?.message || e}`, "error");
    }
  }, [log]);

  const onRandomUser = useCallback(() => {
    setUserId("user-" + Math.random().toString(36).slice(2, 8));
    log("已生成随机用户 ID", "success");
  }, [log]);

  const onQuickFillLocal = useCallback(() => {
    setEndpoint("http://localhost:8080");
    setAppId("nextjs-example-app-id");
    setAppName("Next.js Example");
    setUserId("user-dev-" + Math.floor(Math.random() * 10000));
    log("已填充本地开发配置", "success");
  }, [log]);

  const onTrackEvent = useCallback(() => {
    track.track("button_click", { buttonId: "test-btn", category: "action" });
    log("上报自定义事件: button_click", "success");
  }, [log]);

  const onBatchEvents = useCallback(() => {
    const now = Date.now();
    [
      { id: "batch_event_1", props: { idx: 1, ts: now } },
      { id: "batch_event_2", props: { idx: 2, ts: now + 1 } },
      { id: "batch_event_3", props: { idx: 3, ts: now + 2 } },
    ].forEach((it) => track.track(it.id, it.props));
    log("已触发 3 个自定义事件用于批量上报", "success");
  }, [log]);

  const onTestError = useCallback(() => {
    try {
      throw new Error("这是一个测试错误，用于验证错误监控功能");
    } catch (e) {
      log("错误已捕获并上报", "error");
    }
  }, [log]);

  const onTestPromiseError = useCallback(() => {
    Promise.reject(new Error("这是一个未处理的 Promise 错误")).catch(() => {
      log("Promise 错误已捕获并上报", "error");
    });
  }, [log]);

  const onTestPv = useCallback(() => {
    log("手动触发 PV（通过路由变化触发）", "info");
    window.history.pushState({}, "", "/test-page");
    setTimeout(() => window.history.pushState({}, "", "/"), 800);
  }, [log]);

  const styles = useMemo(
    () => ({
      container: {
        maxWidth: 1000,
        margin: "0 auto",
        padding: 20,
      },
      section: { margin: "20px 0" },
      button: {
        padding: "10px 20px",
        margin: "5px 0",
        fontSize: 14,
        cursor: "pointer",
        border: "none",
        borderRadius: 4,
        background: "#007bff",
        color: "#fff",
      } as React.CSSProperties,
      danger: { background: "#dc3545" },
      success: { background: "#28a745" },
      inputGroup: { margin: "15px 0" },
      status: {
        padding: 10,
        margin: "10px 0",
        borderRadius: 4,
        fontWeight: 500,
      },
      statusColor: {
        success: {
          background: "#d4edda",
          color: "#155724",
          border: "1px solid #c3e6cb",
        },
        error: {
          background: "#f8d7da",
          color: "#721c24",
          border: "1px solid #f5c6cb",
        },
        info: {
          background: "#d1ecf1",
          color: "#0c5460",
          border: "1px solid #bee5eb",
        },
      } as Record<string, React.CSSProperties>,
      log: {
        marginTop: 20,
        padding: 15,
        background: "#f8f9fa",
        borderRadius: 4,
        fontFamily: "Courier New, monospace",
        fontSize: 12,
        whiteSpace: "pre-wrap" as const,
        maxHeight: 300,
        overflowY: "auto" as const,
        border: "1px solid #dee2e6",
      },
      grid2: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
      buttonGroup: { display: "flex", flexWrap: "wrap" as const, gap: 10 },
    }),
    [],
  );

  return (
    <div style={styles.container}>
      <h1>🚀 Track SDK - Next.js 示例</h1>

      <div style={styles.section}>
        <h2>⚙️ SDK 配置</h2>
        <div
          style={{
            ...styles.inputGroup,
            background: "#fff3cd",
            padding: 15,
            borderRadius: 4,
            borderLeft: "4px solid #ffc107",
          }}
        >
          <label style={{ fontWeight: 700, color: "#856404" }}>
            🌐 服务端地址 (Endpoint){" "}
            <span style={{ color: "#dc3545" }}>*必填</span>
          </label>
          <input
            value={endpoint}
            onChange={(e) => setEndpoint(e.target.value)}
            placeholder="http://localhost:8080"
            style={{
              width: "100%",
              padding: "8px 12px",
              border: "2px solid #ffc107",
              borderRadius: 4,
              fontSize: 14,
            }}
          />
          <small style={{ color: "#856404", display: "block", marginTop: 5 }}>
            示例：<code>http://localhost:8080</code> 或{" "}
            <code>https://track.yourdomain.com</code>
          </small>
        </div>
        <div style={styles.grid2 as React.CSSProperties}>
          <div style={styles.inputGroup}>
            <label>应用 ID (App ID)</label>
            <input
              value={appId}
              onChange={(e) => setAppId(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #ddd",
                borderRadius: 4,
                fontSize: 14,
              }}
            />
          </div>
          <div style={styles.inputGroup}>
            <label>项目名 (App Name，可选)</label>
            <input
              value={appName}
              onChange={(e) => setAppName(e.target.value)}
              placeholder="不填则使用 App ID"
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #ddd",
                borderRadius: 4,
                fontSize: 14,
              }}
            />
          </div>
        </div>
        <div style={styles.grid2 as React.CSSProperties}>
          <div style={styles.inputGroup}>
            <label>用户 ID (User ID)</label>
            <input
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #ddd",
                borderRadius: 4,
                fontSize: 14,
              }}
            />
          </div>
        </div>
        <div style={styles.inputGroup}>
          <label>Session 有效期 (分钟)</label>
          <input
            type="number"
            value={sessionTTL}
            onChange={(e) => setSessionTTL(parseInt(e.target.value) || 1440)}
            style={{
              width: "100%",
              padding: "8px 12px",
              border: "1px solid #ddd",
              borderRadius: 4,
              fontSize: 14,
            }}
          />
        </div>
        <div style={styles.buttonGroup}>
          <button
            style={{ ...styles.button, ...styles.success }}
            onClick={onInit}
            disabled={inited}
          >
            初始化 SDK
          </button>
          <button
            style={styles.button}
            onClick={onStart}
            disabled={!inited || started}
          >
            启动追踪
          </button>
          <button
            style={{ ...styles.button, ...styles.danger }}
            onClick={onStop}
            disabled={!started}
          >
            停止追踪
          </button>
          <button style={styles.button} onClick={onQuickFillLocal}>
            一键填充本地
          </button>
          <button style={styles.button} onClick={onRandomUser}>
            随机用户
          </button>
        </div>
        <div
          style={{
            ...styles.status,
            ...(styles.statusColor as any)[status.type],
          }}
        >
          {status.text}
        </div>
      </div>

      <div style={styles.section}>
        <h2>📊 自动采集与测试</h2>
        <div style={styles.buttonGroup}>
          <button
            style={styles.button}
            disabled={!started}
            onClick={() => log("点击了测试按钮（会被自动采集）", "info")}
          >
            测试点击采集
          </button>
          <button style={styles.button} disabled={!started} onClick={onTestPv}>
            手动触发 PV
          </button>
          <button
            style={{ ...styles.button, ...styles.danger }}
            disabled={!started}
            onClick={onTestError}
          >
            触发测试错误
          </button>
          <button
            style={{ ...styles.button, ...styles.danger }}
            disabled={!started}
            onClick={onTestPromiseError}
          >
            触发 Promise 错误
          </button>
        </div>
      </div>

      <div style={styles.section}>
        <h2>🎯 自定义事件上报</h2>
        <div style={styles.buttonGroup}>
          <button
            style={styles.button}
            disabled={!started}
            onClick={onTrackEvent}
          >
            上报自定义事件
          </button>
          <button
            style={styles.button}
            disabled={!started}
            onClick={onBatchEvents}
          >
            批量上报 3 个事件
          </button>
        </div>
      </div>

      <div style={styles.section}>
        <h2>📋 操作日志</h2>
        <div ref={logRef} style={styles.log} />
        <button style={styles.button} onClick={clearLog}>
          清空日志
        </button>
      </div>
    </div>
  );
}
