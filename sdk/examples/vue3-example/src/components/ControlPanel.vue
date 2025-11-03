<template>
  <div class="control-panel">
    <h2>控制面板</h2>
    <div class="panel-section">
      <div class="status-item">
        <span class="label">SDK 状态：</span>
        <span :class="['status-badge', isTracking ? 'active' : 'inactive']">
          {{ isTracking ? '运行中' : '已停止' }}
        </span>
      </div>
      <div class="button-group">
        <button @click="handleStart" :disabled="isTracking" class="btn btn-primary">
          启动采集
        </button>
        <button @click="handleStop" :disabled="!isTracking" class="btn btn-danger">
          停止采集
        </button>
        <button @click="handleFlush" class="btn btn-secondary">
          立即上报
        </button>
      </div>
    </div>
    <div class="panel-section">
      <div class="info-item">
        <span class="label">会话 ID：</span>
        <code>{{ sessionId }}</code>
      </div>
      <div class="info-item">
        <span class="label">匿名 ID：</span>
        <code>{{ anonymousId }}</code>
      </div>
      <div class="info-item">
        <span class="label">用户模式：</span>
        <span :class="['badge', userMode === 'identified' ? 'success' : 'info']">
          {{ userMode === 'identified' ? '实名模式' : '匿名模式' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getTracker } from '../tracker';
import type { Tracker } from '@track/sdk';

const tracker = getTracker();
const isTracking = ref(false);
const sessionId = ref('');
const anonymousId = ref('');
const userMode = ref<'identified' | 'anonymous'>('anonymous');

let updateInterval: number | null = null;

const updateStatus = () => {
  sessionId.value = tracker.getSessionId();
  anonymousId.value = tracker.getAnonymousId();
  userMode.value = tracker.getUserMode();
};

const handleStart = () => {
  tracker.start();
  isTracking.value = true;
};

const handleStop = () => {
  tracker.stop();
  isTracking.value = false;
};

const handleFlush = () => {
  tracker.flush();
};

onMounted(() => {
  isTracking.value = true; // SDK 默认自动启动
  updateStatus();
  // 定期更新状态
  updateInterval = window.setInterval(updateStatus, 1000);
});

onUnmounted(() => {
  if (updateInterval) {
    clearInterval(updateInterval);
  }
});
</script>

<style scoped>
.control-panel {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
}

.panel-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.panel-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.status-item,
.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 500;
  margin-right: 8px;
  min-width: 100px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #e6f7e6;
  color: #52c41a;
}

.status-badge.inactive {
  background: #fff1e6;
  color: #fa8c16;
}

.badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge.success {
  background: #e6f7e6;
  color: #52c41a;
}

.badge.info {
  background: #e6f4ff;
  color: #1890ff;
}

code {
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  word-break: break-all;
}

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #ff7875;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #d9d9d9;
}
</style>

