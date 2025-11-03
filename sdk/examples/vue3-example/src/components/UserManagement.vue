<template>
  <div class="user-management">
    <h2>用户管理</h2>
    
    <div class="current-user">
      <h3>当前用户状态</h3>
      <div class="user-info">
        <div class="info-item">
          <span class="label">用户 ID：</span>
          <code v-if="userId">{{ userId }}</code>
          <span v-else class="text-muted">未设置</span>
        </div>
        <div class="info-item">
          <span class="label">用户名：</span>
          <span v-if="userName">{{ userName }}</span>
          <span v-else class="text-muted">未设置</span>
        </div>
        <div class="info-item">
          <span class="label">邮箱：</span>
          <span v-if="email">{{ email }}</span>
          <span v-else class="text-muted">未设置</span>
        </div>
        <div class="info-item">
          <span class="label">用户模式：</span>
          <span :class="['badge', userMode === 'identified' ? 'success' : 'info']">
            {{ userMode === 'identified' ? '实名模式' : '匿名模式' }}
          </span>
        </div>
      </div>
    </div>

    <div class="user-form">
      <h3>设置用户身份</h3>
      <div class="form-item">
        <label>用户 ID：</label>
        <input v-model="form.userId" type="text" placeholder="例如：123" class="input" />
      </div>
      <div class="form-item">
        <label>用户名：</label>
        <input v-model="form.userName" type="text" placeholder="例如：John Doe" class="input" />
      </div>
      <div class="form-item">
        <label>邮箱：</label>
        <input v-model="form.email" type="email" placeholder="例如：john@example.com" class="input" />
      </div>
      <div class="form-item">
        <label>手机号：</label>
        <input v-model="form.phone" type="text" placeholder="例如：13800138000" class="input" />
      </div>
      <div class="button-group">
        <button @click="handleSetUser" class="btn btn-primary" :disabled="!form.userId">
          设置用户
        </button>
        <button @click="handleClearUser" class="btn btn-danger">
          清除用户
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getTracker } from '../tracker';

const tracker = getTracker();
const userId = ref<string | number | null>(null);
const userName = ref<string>('');
const email = ref<string>('');
const userMode = ref<'identified' | 'anonymous'>('anonymous');

const form = ref({
  userId: '',
  userName: '',
  email: '',
  phone: '',
});

let updateInterval: number | null = null;

const updateUserInfo = () => {
  userId.value = tracker.getUserId();
  userMode.value = tracker.getUserMode();
  // 注意：SDK 不存储 userName 和 email，这里只是演示
  // 实际应用中这些信息可能需要从其他地方获取
};

const handleSetUser = () => {
  if (!form.value.userId.trim()) {
    alert('请输入用户 ID');
    return;
  }

  tracker.setUser({
    userId: form.value.userId,
    userName: form.value.userName || undefined,
    email: form.value.email || undefined,
    phone: form.value.phone || undefined,
  });

  updateUserInfo();
  alert('用户身份设置成功');
};

const handleClearUser = () => {
  tracker.clearUser();
  updateUserInfo();
  form.value = {
    userId: '',
    userName: '',
    email: '',
    phone: '',
  };
  alert('已清除用户身份，切换到匿名模式');
};

onMounted(() => {
  updateUserInfo();
  updateInterval = window.setInterval(updateUserInfo, 1000);
});

onUnmounted(() => {
  if (updateInterval) {
    clearInterval(updateInterval);
  }
});
</script>

<style scoped>
.user-management {
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

.current-user {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.user-info {
  margin-top: 16px;
}

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

code {
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

.text-muted {
  color: #999;
  font-size: 14px;
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

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: #1890ff;
}

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 24px;
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
</style>

