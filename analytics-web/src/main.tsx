import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// 注意：移除 StrictMode 会失去一些有用的开发警告
// 生产环境构建时 StrictMode 不会影响性能
createRoot(document.getElementById('root')!).render(<App />)
