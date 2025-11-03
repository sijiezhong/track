export const config = {
  appTitle: import.meta.env.VITE_APP_TITLE || 'Track 埋点分析平台',
  // 开发环境使用代理，生产环境使用环境变量
  apiBaseUrl: import.meta.env.DEV ? '/api/v1' : (import.meta.env.VITE_API_BASE_URL || '/api/v1'),
  bigScreenSize: {
    width: 1920,
    height: 1080,
  },
} as const
