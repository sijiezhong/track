<template>
  <div class="event-demo">
    <h2>事件上报演示</h2>
    
    <div class="demo-form">
      <div class="form-item">
        <label>事件类型：</label>
        <input
          v-model="eventType"
          type="text"
          placeholder="例如：custom_event"
          class="input"
        />
      </div>
      <div class="form-item">
        <label>事件内容（JSON）：</label>
        <textarea
          v-model="eventContentText"
          placeholder='{"key": "value", "number": 123}'
          class="textarea"
          rows="6"
        ></textarea>
      </div>
      <button @click="handleSubmit" class="btn btn-primary" :disabled="!eventType">
        提交事件
      </button>
    </div>

    <div class="events-list">
      <h3>最近上报的事件</h3>
      <div v-if="events.length === 0" class="empty">暂无事件</div>
      <div v-else class="event-item" v-for="(event, index) in events" :key="index">
        <div class="event-header">
          <span class="event-type">{{ event.event_type }}</span>
          <span class="event-time">{{ event.time }}</span>
        </div>
        <div class="event-content">
          <pre>{{ JSON.stringify(event.event_content, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getTracker } from '../tracker';

const tracker = getTracker();
const eventType = ref('custom_event');
const eventContentText = ref('{"key": "value", "number": 123}');
const events = ref<Array<{ event_type: string; event_content: any; time: string }>>([]);

const handleSubmit = () => {
  if (!eventType.value.trim()) {
    alert('请输入事件类型');
    return;
  }

  let eventContent: Record<string, unknown> = {};
  if (eventContentText.value.trim()) {
    try {
      eventContent = JSON.parse(eventContentText.value);
    } catch (e) {
      alert('事件内容必须是有效的 JSON 格式');
      return;
    }
  }

  // 上报事件
  tracker.trackEvent(eventType.value, eventContent);

  // 添加到事件列表
  events.value.unshift({
    event_type: eventType.value,
    event_content: eventContent,
    time: new Date().toLocaleTimeString(),
  });

  // 限制列表长度
  if (events.value.length > 10) {
    events.value = events.value.slice(0, 10);
  }

  // 清空输入
  eventType.value = 'custom_event';
  eventContentText.value = '{"key": "value"}';
};
</script>

<style scoped>
.event-demo {
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
  margin-top: 32px;
  margin-bottom: 16px;
}

.demo-form {
  margin-bottom: 24px;
}

.form-item {
  margin-bottom: 16px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.input,
.textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}

.textarea {
  font-family: 'Monaco', 'Courier New', monospace;
  resize: vertical;
}

.input:focus,
.textarea:focus {
  outline: none;
  border-color: #1890ff;
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

.events-list {
  margin-top: 24px;
}

.empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

.event-item {
  background: #f5f5f5;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.event-type {
  font-weight: 600;
  color: #1890ff;
}

.event-time {
  font-size: 12px;
  color: #999;
}

.event-content pre {
  margin: 0;
  font-size: 12px;
  color: #666;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

