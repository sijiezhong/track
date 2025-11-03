<template>
  <nav class="navigation">
    <div class="nav-container">
      <router-link to="/" class="nav-link" active-class="active">
        <span class="nav-icon">🏠</span>
        <span class="nav-text">首页</span>
      </router-link>
      <router-link to="/analytics" class="nav-link" active-class="active">
        <span class="nav-icon">📊</span>
        <span class="nav-text">PV/UV 统计</span>
      </router-link>
    </div>
  </nav>
</template>

<script setup lang="ts">
// No additional logic needed for now
</script>

<style scoped>
.navigation {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 8px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  text-decoration: none;
  color: #666;
  font-weight: 500;
  font-size: 15px;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
  position: relative;
}

.nav-link:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.05);
}

.nav-link.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.nav-icon {
  font-size: 18px;
}

.nav-text {
  font-size: 14px;
}

@media (max-width: 768px) {
  .nav-container {
    padding: 0 16px;
  }
  
  .nav-link {
    padding: 12px 16px;
  }
  
  .nav-text {
    display: none;
  }
  
  .nav-icon {
    font-size: 20px;
  }
}
</style>

