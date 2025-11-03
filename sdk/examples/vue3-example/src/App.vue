<template>
  <div class="app">
    <header class="app-header">
      <h1>Track SDK 功能演示</h1>
      <p class="subtitle">展示 SDK 的各项功能和使用方法</p>
    </header>

    <Navigation />

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import Navigation from './components/Navigation.vue';
</script>

<style scoped>
.app {
  min-height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 24px;
  text-align: center;
}

.app-header h1 {
  margin: 0 0 8px 0;
  font-size: 32px;
  font-weight: 700;
}

.subtitle {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
}

.app-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}
</style>
