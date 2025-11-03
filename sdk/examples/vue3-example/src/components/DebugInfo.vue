<template>
  <div class="debug-info">
    <h2>调试信息</h2>
    
    <div class="info-section">
      <h3>会话信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="label">会话 ID：</span>
          <code>{{ sessionId }}</code>
          <button @click="copyToClipboard(sessionId)" class="btn-copy">复制</button>
        </div>
        <div class="info-item">
          <span class="label">匿名 ID：</span>
          <code>{{ anonymousId }}</code>
          <button @click="copyToClipboard(anonymousId)" class="btn-copy">复制</button>
        </div>
        <div class="info-item">
          <span class="label">用户 ID：</span>
          <code v-if="userId">{{ userId }}</code>
          <span v-else class="text-muted">未设置</span>
        </div>
      </div>
    </div>

    <div class="info-section">
      <h3>队列状态</h3>
      <div class="info-item">
        <span class="label">队列长度：</span>
        <span class="value">{{ queueSize }}</span>
      </div>
    </div>

    <div class="info-section">
      <h3>SDK 配置</h3>
      <div class="config-info">
        <div class="config-item">
          <span class="label">API 地址：</span>
          <code>{{ endpoint }}</code>
        </div>
        <div class="config-item">
          <span class="label">项目 ID：</span>
          <code>{{ projectId }}</code>
        </div>
        <div class="config-item">
          <span class="label">批量阈值：</span>
          <code>{{ batchSize }}</code>
        </div>
        <div class="config-item">
          <span class="label">超时时间：</span>
          <code>{{ batchTimeout }}ms</code>
        </div>
      </div>
    </div>

    <div class="info-section">
      <h3>最近日志</h3>
      <div class="log-container">
        <div v-if="logs.length === 0" class="empty">暂无日志</div>
        <div v-else class="log-item" v-for="(log, index) in logs" :key="index">
          <span class="log-time">{{ log.time }}</span>
          <span :class="['log-level', log.level]">{{ log.level.toUpperCase() }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getTracker } from '../tracker';
import { SDK_CONFIG } from '../config';

const tracker = getTracker();
const sessionId = ref('');
const anonymousId = ref('');
const userId = ref<string | number | null>(null);
const queueSize = ref(0);
const logs = ref<Array<{ time: string; level: string; message: string }>>([]);

const endpoint = SDK_CONFIG.endpoint;
const projectId = SDK_CONFIG.projectId;
const batchSize = SDK_CONFIG.batchSize;
const batchTimeout = SDK_CONFIG.batchTimeout;

const updateInfo = () => {
  sessionId.value = tracker.getSessionId();
  anonymousId.value = tracker.getAnonymousId();
  userId.value = tracker.getUserId();
  // 注意：SDK 没有公开的队列大小接口，这里仅作演示
  // 实际可以通过内部实现或事件监听获取
};

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    alert('已复制到剪贴板');
  }).catch(() => {
    alert('复制失败');
  });
};

// 拦截 console 日志（仅用于演示）
const originalConsoleLog = console.log;
const originalConsoleInfo = console.info;
const originalConsoleWarn = console.warn;
const originalConsoleError = console.error;

const addLog = (level: string, ...args: unknown[]) => {
  logs.value.unshift({
    time: new Date().toLocaleTimeString(),
    level,
    message: args.map(arg => 
      typeof arg === 'object' ? JSON.stringify(arg, null, 2) : String(arg)
    ).join(' '),
  });
  
  // 限制日志数量
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50);
  }
};

onMounted(() => {
  updateInfo();
  const interval = window.setInterval(updateInfo, 1000);

  // 拦截日志（仅开发环境）
  console.log = (...args) => {
    originalConsoleLog.apply(console, args);
    addLog('log', ...args);
  };
  console.info = (...args) => {
    originalConsoleInfo.apply(console, args);
    addLog('info', ...args);
  };
  console.warn = (...args) => {
    originalConsoleWarn.apply(console, args);
    addLog('warn', ...args);
  };
  console.error = (...args) => {
    originalConsoleError.apply(console, args);
    addLog('error', ...args);
  };

  return () => {
    clearInterval(interval);
    console.log = originalConsoleLog;
    console.info = originalConsoleInfo;
    console.warn = originalConsoleWarn;
    console.error = originalConsoleError;
  };
});

onUnmounted(() => {
  // 恢复原始 console
  console.log = originalConsoleLog;
  console.info = originalConsoleInfo;
  console.warn = originalConsoleWarn;
  console.error = originalConsoleError;
});
</script>

<style scoped>
.debug-info {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

h2,
h3 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
}

h3 {
  font-size: 16px;
}

.info-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.info-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.info-grid {
  display: grid;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-weight: 500;
  min-width: 100px;
}

code {
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  flex: 1;
}

.text-muted {
  color: #999;
  font-size: 14px;
}

.value {
  font-weight: 600;
  font-size: 16px;
  color: #1890ff;
}

.btn-copy {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-copy:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.config-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-container {
  max-height: 400px;
  overflow-y: auto;
  background: #1e1e1e;
  border-radius: 6px;
  padding: 12px;
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

.empty {
  color: #999;
  text-align: center;
  padding: 20px;
}

.log-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  color: #d4d4d4;
}

.log-time {
  color: #808080;
  min-width: 80px;
}

.log-level {
  min-width: 50px;
  font-weight: 600;
}

.log-level.log {
  color: #d4d4d4;
}

.log-level.info {
  color: #4ec9b0;
}

.log-level.warn {
  color: #dcdcaa;
}

.log-level.error {
  color: #f48771;
}

.log-message {
  flex: 1;
  word-break: break-all;
}
</style>

