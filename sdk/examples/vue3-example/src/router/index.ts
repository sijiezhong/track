import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import Analytics from '../views/Analytics.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: {
      title: 'SDK 演示',
    },
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: Analytics,
    meta: {
      title: 'PV/UV 统计',
    },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫：更新页面标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - Track SDK`;
  }
  next();
});

export default router;

