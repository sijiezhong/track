import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { initializeTracker } from './tracker'

// 初始化 SDK
initializeTracker();

const app = createApp(App)
app.use(router)
app.mount('#app')
