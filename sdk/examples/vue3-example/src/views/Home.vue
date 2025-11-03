<template>
  <div class="home">
    <div class="content-grid">
      <div class="panel">
        <ControlPanel />
      </div>
      
      <div class="panel">
        <UserManagement />
      </div>
      
      <div class="panel panel-full">
        <EventDemo />
      </div>
      
      <div class="panel">
        <TestButtons />
      </div>
      
      <div class="panel panel-full">
        <DebugInfo />
      </div>
      
      <div class="panel panel-full">
        <EventList />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ControlPanel from '../components/ControlPanel.vue';
import EventDemo from '../components/EventDemo.vue';
import UserManagement from '../components/UserManagement.vue';
import DebugInfo from '../components/DebugInfo.vue';
import TestButtons from '../components/TestButtons.vue';
import EventList from '../components/EventList.vue';
</script>

<style scoped>
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.panel {
  min-height: 200px;
}

.panel-full {
  grid-column: 1 / -1;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .panel-full {
    grid-column: 1;
  }
}
</style>

