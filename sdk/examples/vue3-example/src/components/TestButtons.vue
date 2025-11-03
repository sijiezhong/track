<template>
  <div class="test-buttons">
    <h2>测试场景</h2>
    <p class="description">点击下方按钮触发不同的测试场景，验证 SDK 的各项功能</p>
    
    <div class="button-grid">
      <button @click="testPageView" class="test-btn btn-pageview">
        <span class="btn-icon">📄</span>
        <span class="btn-text">页面访问测试</span>
      </button>
      
      <button @click="testClick" class="test-btn btn-click">
        <span class="btn-icon">🖱️</span>
        <span class="btn-text">点击事件测试</span>
      </button>
      
      <button @click="testError" class="test-btn btn-error">
        <span class="btn-icon">⚠️</span>
        <span class="btn-text">错误事件测试</span>
      </button>
      
      <button @click="testPerformance" class="test-btn btn-performance">
        <span class="btn-icon">⚡</span>
        <span class="btn-text">性能数据测试</span>
      </button>
      
      <button @click="testBatch" class="test-btn btn-batch">
        <span class="btn-icon">📦</span>
        <span class="btn-text">批量上报测试</span>
      </button>
      
      <button @click="testCustomEvent" class="test-btn btn-custom">
        <span class="btn-icon">🎯</span>
        <span class="btn-text">自定义事件测试</span>
      </button>
    </div>

    <div v-if="testResult" class="test-result">
      <h3>测试结果</h3>
      <div :class="['result-message', testResult.type]">
        {{ testResult.message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getTracker } from '../tracker';

const tracker = getTracker();
const testResult = ref<{ type: 'success' | 'info'; message: string } | null>(null);

const showResult = (type: 'success' | 'info', message: string) => {
  testResult.value = { type, message };
  setTimeout(() => {
    testResult.value = null;
  }, 3000);
};

const testPageView = () => {
  tracker.trackPageView();
  showResult('success', '页面访问事件已上报');
};

const testClick = () => {
  tracker.trackEvent('click', {
    element: 'test-button',
    position: { x: 100, y: 200 },
    timestamp: Date.now(),
  });
  showResult('success', '点击事件已上报');
};

const testError = () => {
  // 触发一个错误来测试错误采集
  // 方案1：不捕获错误，让它触发全局 error 事件
  // 方案2：捕获错误后手动上报（推荐，不会污染控制台）
  try {
    // @ts-ignore
    undefinedMethod();
  } catch (error) {
    // 手动上报错误事件，模拟错误采集器的行为
    const errorObj = error instanceof Error ? error : new Error(String(error));
    tracker.trackEvent('error', {
      errorType: 'javascript',
      message: errorObj.message || 'Unknown error',
      stack: errorObj.stack,
      filename: window.location.href,
      lineno: undefined,
      colno: undefined,
    });
    showResult('success', '错误事件已上报');
  }
};

const testPerformance = () => {
  tracker.trackEvent('performance', {
    loadTime: performance.timing.loadEventEnd - performance.timing.navigationStart,
    domContentLoaded: performance.timing.domContentLoadedEventEnd - performance.timing.navigationStart,
  });
  showResult('success', '性能数据已上报');
};

const testBatch = () => {
  // 上报多个事件，测试批量上报
  for (let i = 0; i < 5; i++) {
    tracker.trackEvent('batch_test', {
      index: i,
      timestamp: Date.now(),
    });
  }
  showResult('success', '已上报 5 个事件，将触发批量上报');
};

const testCustomEvent = () => {
  tracker.trackEvent('test_custom', {
    category: 'test',
    action: 'button_click',
    label: 'custom_event_test',
    value: Math.random() * 100,
    timestamp: Date.now(),
  });
  showResult('success', '自定义事件已上报');
};
</script>

<style scoped>
.test-buttons {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

h3 {
  margin: 24px 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.test-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.test-btn:hover {
  border-color: #1890ff;
  background: #f0f8ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
}

.btn-icon {
  font-size: 32px;
}

.btn-text {
  font-weight: 500;
}

.test-result {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.result-message {
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 14px;
}

.result-message.success {
  background: #e6f7e6;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.result-message.info {
  background: #e6f4ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
</style>

