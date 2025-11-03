<template>
  <div class="analytics">
    <div class="analytics-header">
      <h2>📊 访问统计分析</h2>
      <p class="description">实时展示页面访问量（PV）和独立访客数（UV）统计信息</p>
    </div>

    <div class="stats-grid">
      <!-- PV 统计 -->
      <div class="stat-card">
        <div class="stat-icon">👁️</div>
        <div class="stat-content">
          <div class="stat-label">总浏览量 (PV)</div>
          <div class="stat-value">{{ stats.totalPageViews }}</div>
          <div class="stat-description">页面访问总次数</div>
        </div>
      </div>

      <!-- UV 统计 -->
      <div class="stat-card">
        <div class="stat-icon">👤</div>
        <div class="stat-content">
          <div class="stat-label">独立访客 (UV)</div>
          <div class="stat-value">{{ stats.uniqueVisitors }}</div>
          <div class="stat-description">唯一访客数（基于 session）</div>
        </div>
      </div>

      <!-- 会话数 -->
      <div class="stat-card">
        <div class="stat-icon">🔄</div>
        <div class="stat-content">
          <div class="stat-label">会话数</div>
          <div class="stat-value">{{ stats.totalSessions }}</div>
          <div class="stat-description">活跃会话总数</div>
        </div>
      </div>

      <!-- 平均访问时长 -->
      <div class="stat-card">
        <div class="stat-icon">⏱️</div>
        <div class="stat-content">
          <div class="stat-label">平均访问时长</div>
          <div class="stat-value">{{ formatDuration(stats.avgDuration) }}</div>
          <div class="stat-description">用户平均停留时间</div>
        </div>
      </div>
    </div>

    <!-- 页面访问明细 -->
    <div class="details-section">
      <h3>📄 页面访问明细</h3>
      <div class="table-container">
        <table class="details-table">
          <thead>
            <tr>
              <th>页面路径</th>
              <th>访问次数</th>
              <th>独立访客</th>
              <th>最后访问时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pageDetails.length === 0">
              <td colspan="4" class="empty-state">暂无访问记录</td>
            </tr>
            <tr v-for="(page, index) in pageDetails" :key="index">
              <td class="page-path">{{ page.path }}</td>
              <td class="stat-cell">{{ page.views }}</td>
              <td class="stat-cell">{{ page.visitors }}</td>
              <td class="time-cell">{{ formatTime(page.lastVisit) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 实时事件流 -->
    <div class="details-section">
      <h3>🔴 实时事件流</h3>
      <div class="events-container">
        <div v-if="recentEvents.length === 0" class="empty-state">
          暂无事件记录
        </div>
        <div v-else class="event-list">
          <div v-for="(event, index) in recentEvents" :key="index" class="event-item">
            <div class="event-type" :class="`event-${event.type}`">
              {{ getEventIcon(event.type) }} {{ event.type }}
            </div>
            <div class="event-info">
              <div class="event-path">{{ event.path }}</div>
              <div class="event-time">{{ formatTime(event.timestamp) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 刷新按钮 -->
    <div class="actions">
      <button @click="refreshStats" class="refresh-btn">
        <span class="refresh-icon">🔄</span>
        刷新数据
      </button>
      <button @click="clearStats" class="clear-btn">
        <span class="clear-icon">🗑️</span>
        清空统计
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { getTracker } from '../tracker';

interface PageStats {
  path: string;
  views: number;
  visitors: Set<string>;
  lastVisit: number;
}

interface EventRecord {
  type: string;
  path: string;
  timestamp: number;
  sessionId: string;
}

const stats = ref({
  totalPageViews: 0,
  uniqueVisitors: 0,
  totalSessions: 0,
  avgDuration: 0,
});

const pageStatsMap = ref<Map<string, PageStats>>(new Map());
const events = ref<EventRecord[]>([]);
const sessions = ref<Set<string>>(new Set());
let refreshTimer: number | null = null;

const tracker = getTracker();

// 计算页面明细
const pageDetails = computed(() => {
  return Array.from(pageStatsMap.value.values())
    .map(page => ({
      path: page.path,
      views: page.views,
      visitors: page.visitors.size,
      lastVisit: page.lastVisit,
    }))
    .sort((a, b) => b.views - a.views);
});

// 最近的事件
const recentEvents = computed(() => {
  return events.value
    .slice()
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 10);
});

// 格式化时长
const formatDuration = (ms: number): string => {
  if (ms === 0) return '0秒';
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  
  if (hours > 0) {
    return `${hours}小时${minutes % 60}分钟`;
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds % 60}秒`;
  } else {
    return `${seconds}秒`;
  }
};

// 格式化时间
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) {
    return '刚刚';
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  } else if (diff < 86400000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else {
    return date.toLocaleString('zh-CN', { 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit', 
      minute: '2-digit'
    });
  }
};

// 获取事件图标
const getEventIcon = (type: string): string => {
  const icons: Record<string, string> = {
    pageview: '📄',
    click: '🖱️',
    error: '⚠️',
    performance: '⚡',
  };
  return icons[type] || '📌';
};

// 记录页面访问
const recordPageView = (path: string) => {
  const sessionId = tracker.getSessionId();
  
  // 更新总访问量
  stats.value.totalPageViews++;
  
  // 更新会话
  if (!sessions.value.has(sessionId)) {
    sessions.value.add(sessionId);
    stats.value.uniqueVisitors++;
    stats.value.totalSessions = sessions.value.size;
  }
  
  // 更新页面统计
  let pageStat = pageStatsMap.value.get(path);
  if (!pageStat) {
    pageStat = {
      path,
      views: 0,
      visitors: new Set(),
      lastVisit: 0,
    };
    pageStatsMap.value.set(path, pageStat);
  }
  
  pageStat.views++;
  pageStat.visitors.add(sessionId);
  pageStat.lastVisit = Date.now();
  
  // 记录事件
  events.value.push({
    type: 'pageview',
    path,
    timestamp: Date.now(),
    sessionId,
  });
  
  // 保存到 localStorage
  saveStats();
};

// 记录其他事件
const recordEvent = (type: string, path: string) => {
  const sessionId = tracker.getSessionId();
  
  events.value.push({
    type,
    path,
    timestamp: Date.now(),
    sessionId,
  });
  
  // 限制事件数量
  if (events.value.length > 100) {
    events.value = events.value.slice(-100);
  }
  
  saveStats();
};

// 保存统计数据到 localStorage
const saveStats = () => {
  try {
    const data = {
      stats: stats.value,
      pages: Array.from(pageStatsMap.value.entries()).map(([path, stat]) => ({
        path,
        views: stat.views,
        visitors: Array.from(stat.visitors),
        lastVisit: stat.lastVisit,
      })),
      events: events.value.slice(-50), // 只保存最近 50 条
      sessions: Array.from(sessions.value),
    };
    localStorage.setItem('track_analytics', JSON.stringify(data));
  } catch (e) {
    console.error('Failed to save stats:', e);
  }
};

// 从 localStorage 加载统计数据
const loadStats = () => {
  try {
    const data = localStorage.getItem('track_analytics');
    if (data) {
      const parsed = JSON.parse(data);
      
      stats.value = parsed.stats || stats.value;
      
      if (parsed.pages) {
        pageStatsMap.value.clear();
        parsed.pages.forEach((page: any) => {
          pageStatsMap.value.set(page.path, {
            path: page.path,
            views: page.views,
            visitors: new Set(page.visitors),
            lastVisit: page.lastVisit,
          });
        });
      }
      
      if (parsed.events) {
        events.value = parsed.events;
      }
      
      if (parsed.sessions) {
        sessions.value = new Set(parsed.sessions);
      }
    }
  } catch (e) {
    console.error('Failed to load stats:', e);
  }
};

// 刷新统计
const refreshStats = () => {
  loadStats();
};

// 清空统计
const clearStats = () => {
  if (confirm('确定要清空所有统计数据吗？')) {
    stats.value = {
      totalPageViews: 0,
      uniqueVisitors: 0,
      totalSessions: 0,
      avgDuration: 0,
    };
    pageStatsMap.value.clear();
    events.value = [];
    sessions.value.clear();
    localStorage.removeItem('track_analytics');
  }
};

// 监听路由变化
const handleRouteChange = () => {
  const path = window.location.pathname;
  recordPageView(path);
};

// 自动刷新
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    // 自动保存
    saveStats();
  }, 5000); // 每 5 秒保存一次
};

onMounted(() => {
  loadStats();
  handleRouteChange();
  startAutoRefresh();
  
  // 监听路由变化（如果使用 vue-router）
  window.addEventListener('popstate', handleRouteChange);
});

onUnmounted(() => {
  if (refreshTimer !== null) {
    clearInterval(refreshTimer);
  }
  window.removeEventListener('popstate', handleRouteChange);
});

// 记录当前页面访问
recordPageView(window.location.pathname);
</script>

<style scoped>
.analytics {
  max-width: 1400px;
  margin: 0 auto;
}

.analytics-header {
  margin-bottom: 32px;
}

.analytics-header h2 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 48px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 4px;
}

.stat-description {
  font-size: 12px;
  color: #999;
}

.details-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.details-section h3 {
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.table-container {
  overflow-x: auto;
}

.details-table {
  width: 100%;
  border-collapse: collapse;
}

.details-table thead {
  background: #f5f5f5;
}

.details-table th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #666;
  font-size: 14px;
  border-bottom: 2px solid #e0e0e0;
}

.details-table td {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.page-path {
  color: #1890ff;
  font-family: monospace;
}

.stat-cell {
  text-align: center;
  font-weight: 600;
  color: #333;
}

.time-cell {
  color: #999;
  font-size: 13px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.events-container {
  max-height: 400px;
  overflow-y: auto;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: background 0.2s;
}

.event-item:hover {
  background: #f0f0f0;
}

.event-type {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.event-pageview {
  background: #e6f7ff;
  color: #1890ff;
}

.event-click {
  background: #f0f5ff;
  color: #597ef7;
}

.event-error {
  background: #fff1f0;
  color: #ff4d4f;
}

.event-performance {
  background: #f6ffed;
  color: #52c41a;
}

.event-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.event-path {
  font-family: monospace;
  font-size: 13px;
  color: #333;
}

.event-time {
  font-size: 12px;
  color: #999;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 24px;
}

.refresh-btn,
.clear-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn {
  background: #1890ff;
  color: white;
}

.refresh-btn:hover {
  background: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.clear-btn {
  background: #fff;
  color: #ff4d4f;
  border: 1px solid #ff4d4f;
}

.clear-btn:hover {
  background: #fff1f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .actions {
    flex-direction: column;
  }
  
  .refresh-btn,
  .clear-btn {
    width: 100%;
  }
}
</style>

