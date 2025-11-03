<template>
  <div class="event-list">
    <div class="header">
      <h2>数据库事件列表</h2>
      <div class="header-actions">
        <input
          v-model="eventNameFilter"
          type="text"
          placeholder="按事件名过滤"
          class="filter-input"
          @input="handleFilterChange"
        />
        <button @click="refresh" class="btn btn-refresh" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="!loading && events.length === 0" class="empty">
      暂无事件数据
    </div>

    <div v-else class="events-container">
      <div class="pagination-info">
        <span>
          共 {{ total }} 条记录，第 {{ page + 1 }} / {{ totalPages }} 页
        </span>
      </div>

      <div class="events-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>事件名</th>
              <th>用户ID</th>
              <th>会话ID</th>
              <th>事件时间</th>
              <th>文本信息</th>
              <th>属性</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="event in events" :key="event.id">
              <td>{{ event.id }}</td>
              <td>
                <span :class="['event-name', getEventNameClass(event.eventName)]">
                  {{ event.eventName }}
                </span>
              </td>
              <td>{{ event.userId || '-' }}</td>
              <td>{{ event.sessionId || '-' }}</td>
              <td>{{ formatTime(event.eventTime) }}</td>
              <td class="text-info">
                {{ extractTextInfo(event) }}
              </td>
              <td>
                <button
                  @click="toggleProperties(event.id)"
                  class="btn-properties"
                  :class="{ expanded: expandedProperties.has(event.id) }"
                >
                  {{ expandedProperties.has(event.id) ? '收起' : '查看' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="expandedProperties.size > 0" class="properties-panel">
          <div
            v-for="event in events.filter((e) => expandedProperties.has(e.id))"
            :key="`props-${event.id}`"
            class="properties-item"
          >
            <h4>事件 #{{ event.id }} 的属性</h4>
            <pre>{{ formatProperties(event.properties) }}</pre>
          </div>
        </div>
      </div>

      <div class="pagination">
        <button
          @click="goToPage(0)"
          :disabled="page === 0 || loading"
          class="btn btn-page"
        >
          首页
        </button>
        <button
          @click="goToPage(page - 1)"
          :disabled="page === 0 || loading"
          class="btn btn-page"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ page + 1 }} / {{ totalPages }} 页
        </span>
        <button
          @click="goToPage(page + 1)"
          :disabled="page >= totalPages - 1 || loading"
          class="btn btn-page"
        >
          下一页
        </button>
        <button
          @click="goToPage(totalPages - 1)"
          :disabled="page >= totalPages - 1 || loading"
          class="btn btn-page"
        >
          末页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { SDK_CONFIG } from '../config';

interface EventListItem {
  id: number;
  eventName: string;
  userId?: number;
  sessionId?: number;
  tenantId: number;
  eventTime: string;
  properties?: string;
}

interface PageResult<T> {
  total: number;
  page: number;
  size: number;
  content: T[];
}

const loading = ref(false);
const error = ref<string | null>(null);
const events = ref<EventListItem[]>([]);
const total = ref(0);
const page = ref(0);
const size = ref(20);
const eventNameFilter = ref('');
const expandedProperties = ref(new Set<number>());

const totalPages = computed(() => Math.ceil(total.value / size.value));

const fetchEvents = async () => {
  loading.value = true;
  error.value = null;

  try {
    const params = new URLSearchParams({
      page: page.value.toString(),
      size: size.value.toString(),
    });

    if (eventNameFilter.value.trim()) {
      params.append('eventName', eventNameFilter.value.trim());
    }

    // 使用相对路径，通过 Vite 代理转发到后端
    const response = await fetch(
      `/api/v1/events?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': SDK_CONFIG.projectId.toString(),
        },
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const result: { code: number; message: string; data: PageResult<EventListItem> } =
      await response.json();

    if (result.code === 200 && result.data) {
      events.value = result.data.content || [];
      total.value = result.data.total || 0;
      page.value = result.data.page || 0;
    } else {
      throw new Error(result.message || '获取事件列表失败');
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取事件列表失败';
    console.error('Failed to fetch events:', err);
  } finally {
    loading.value = false;
  }
};

const refresh = () => {
  page.value = 0;
  fetchEvents();
};

const goToPage = (newPage: number) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    page.value = newPage;
    fetchEvents();
  }
};

const handleFilterChange = () => {
  // 防抖处理
  if (filterTimeout) {
    clearTimeout(filterTimeout);
  }
  filterTimeout = window.setTimeout(() => {
    page.value = 0;
    fetchEvents();
  }, 500);
};

let filterTimeout: number | null = null;

const toggleProperties = (eventId: number) => {
  if (expandedProperties.value.has(eventId)) {
    expandedProperties.value.delete(eventId);
  } else {
    expandedProperties.value.add(eventId);
  }
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '-';
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

const formatProperties = (properties?: string) => {
  if (!properties) return '{}';
  try {
    const parsed = JSON.parse(properties);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return properties;
  }
};

const getEventNameClass = (eventName: string) => {
  const classMap: Record<string, string> = {
    pageview: 'event-pageview',
    click: 'event-click',
    error: 'event-error',
    performance: 'event-performance',
  };
  return classMap[eventName] || 'event-custom';
};

/**
 * 从事件属性中提取文本信息
 */
const extractTextInfo = (event: EventListItem): string => {
  if (!event.properties) {
    return '-';
  }

  try {
    const props = JSON.parse(event.properties);
    const eventName = event.eventName.toLowerCase();

    // 根据不同事件类型提取文本信息
    switch (eventName) {
      case 'pageview':
        // 页面访问：显示标题或URL
        if (props.title) {
          return props.title;
        }
        if (props.url) {
          return props.url;
        }
        return '-';

      case 'click':
        // 点击事件：显示文本内容或元素信息
        if (props.text) {
          return props.text.length > 50 ? props.text.substring(0, 50) + '...' : props.text;
        }
        if (props.selector) {
          return props.selector;
        }
        if (props.element) {
          return props.element;
        }
        if (props.tag) {
          return `点击了 ${props.tag}`;
        }
        return '-';

      case 'error':
        // 错误事件：显示错误消息
        if (props.message) {
          return props.message.length > 50 ? props.message.substring(0, 50) + '...' : props.message;
        }
        if (props.errorType) {
          return `错误类型: ${props.errorType}`;
        }
        return '-';

      case 'performance':
        // 性能事件：显示关键指标
        const metrics: string[] = [];
        if (props.loadTime) {
          metrics.push(`加载: ${props.loadTime}ms`);
        }
        if (props.firstPaint) {
          metrics.push(`首绘: ${props.firstPaint}ms`);
        }
        return metrics.length > 0 ? metrics.join(', ') : '-';

      default:
        // 自定义事件：尝试提取常见的文本字段
        const textFields = ['text', 'message', 'content', 'value', 'name', 'title', 'description'];
        for (const field of textFields) {
          if (props[field] && typeof props[field] === 'string') {
            const text = props[field];
            return text.length > 50 ? text.substring(0, 50) + '...' : text;
          }
        }
        // 如果没有找到文本字段，返回一个摘要
        const keys = Object.keys(props);
        if (keys.length > 0) {
          return `包含 ${keys.length} 个属性`;
        }
        return '-';
    }
  } catch (e) {
    // JSON 解析失败，返回原始字符串的截取
    const propsStr = event.properties;
    return propsStr.length > 50 ? propsStr.substring(0, 50) + '...' : propsStr;
  }
};

onMounted(() => {
  fetchEvents();
  // 每 5 秒自动刷新一次
  const interval = setInterval(() => {
    if (!loading.value) {
      fetchEvents();
    }
  }, 5000);

  return () => {
    clearInterval(interval);
    if (filterTimeout !== null) {
      clearTimeout(filterTimeout);
    }
  };
});
</script>

<style scoped>
.event-list {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  min-width: 200px;
}

.filter-input:focus {
  outline: none;
  border-color: #1890ff;
}

.error-message {
  padding: 12px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  color: #cf1322;
  margin-bottom: 16px;
}

.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.events-container {
  margin-top: 16px;
}

.pagination-info {
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.events-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  background: #fafafa;
}

th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  border-bottom: 2px solid #e8e8e8;
  white-space: nowrap;
}

td {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.text-info {
  max-width: 300px;
  word-break: break-word;
  color: #666;
  font-size: 13px;
}

.event-name {
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
  font-size: 12px;
}

.event-pageview {
  background: #e6f7ff;
  color: #1890ff;
}

.event-click {
  background: #f6ffed;
  color: #52c41a;
}

.event-error {
  background: #fff1f0;
  color: #cf1322;
}

.event-performance {
  background: #fff7e6;
  color: #fa8c16;
}

.event-custom {
  background: #f0f0f0;
  color: #666;
}

.btn-properties {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-properties:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.btn-properties.expanded {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.properties-panel {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.properties-item {
  margin-bottom: 16px;
}

.properties-item:last-child {
  margin-bottom: 0;
}

.properties-item h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
}

.properties-item pre {
  margin: 0;
  padding: 12px;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-info {
  padding: 0 12px;
  color: #666;
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

.btn-refresh {
  background: #1890ff;
  color: white;
}

.btn-refresh:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-page {
  background: #f0f0f0;
  color: #333;
  padding: 6px 12px;
}

.btn-page:hover:not(:disabled) {
  background: #d9d9d9;
}
</style>

