import { Link, useLocation, Outlet } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { usePermission } from '@/hooks/usePermission'
import { ROUTES } from '@/constants/routes'
import { cn } from '@/lib/utils'
import {
  TrendingUp,
  Filter,
  Users,
  GitBranch,
  PieChart,
  Activity,
  Home,
  Settings,
  Webhook,
  FileText,
  ChevronLeft,
} from 'lucide-react'

interface MenuItem {
  title: string
  icon: React.ElementType
  path: string
  requiredRoles?: string[]
}

export function AppLayout() {
  const location = useLocation()
  const { user } = useAuth()
  const { canAccess } = usePermission()

  const menuItems: MenuItem[] = [
    {
      title: '数据大屏',
      icon: Home,
      path: ROUTES.HOME,
    },
    {
      title: '趋势分析',
      icon: TrendingUp,
      path: ROUTES.ANALYTICS.TREND,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '漏斗分析',
      icon: Filter,
      path: ROUTES.ANALYTICS.FUNNEL,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '留存分析',
      icon: Users,
      path: ROUTES.ANALYTICS.RETENTION,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '路径分析',
      icon: GitBranch,
      path: ROUTES.ANALYTICS.PATH,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '分群分析',
      icon: PieChart,
      path: ROUTES.ANALYTICS.SEGMENTATION,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '热点图',
      icon: Activity,
      path: ROUTES.ANALYTICS.HEATMAP,
      requiredRoles: ['ADMIN', 'ANALYST'],
    },
    {
      title: '用户管理',
      icon: Users,
      path: ROUTES.ADMIN.USERS,
      requiredRoles: ['ADMIN'],
    },
    {
      title: '应用管理',
      icon: Settings,
      path: ROUTES.ADMIN.APPLICATIONS,
      requiredRoles: ['ADMIN'],
    },
    {
      title: 'Webhook',
      icon: Webhook,
      path: ROUTES.ADMIN.WEBHOOKS,
      requiredRoles: ['ADMIN'],
    },
    {
      title: '操作日志',
      icon: FileText,
      path: ROUTES.ADMIN.AUDIT_LOG,
      requiredRoles: ['ADMIN'],
    },
  ]

  const visibleMenuItems = menuItems.filter((item) => {
    if (!item.requiredRoles) return true
    return canAccess(item.requiredRoles as any)
  })

  return (
    <div className="flex h-screen bg-background">
      {/* 侧边栏 */}
      <aside className="w-64 border-r bg-card">
        <div className="flex h-full flex-col">
          {/* Logo */}
          <div className="flex h-16 items-center border-b px-6">
            <h1 className="text-xl font-bold">Track 分析平台</h1>
          </div>

          {/* 导航菜单 */}
          <nav className="flex-1 space-y-1 overflow-y-auto p-4">
            {visibleMenuItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname === item.path

              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={cn(
                    'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {item.title}
                </Link>
              )
            })}
          </nav>

          {/* 用户信息 */}
          {user && (
            <div className="border-t p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs text-primary-foreground">
                  {user.username?.substring(0, 2).toUpperCase()}
                </div>
                <div className="flex-1 text-sm">
                  <div className="font-medium">{user.username}</div>
                  <div className="text-xs text-muted-foreground">应用 ID: {user.appId}</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* 主内容区 */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
