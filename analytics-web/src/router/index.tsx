import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ROUTES } from '@/constants/routes'
import { RouteGuard } from '@/components/auth/RouteGuard'
import { AppLayout } from '@/components/layout/AppLayout'

// 懒加载页面组件
const Login = lazy(() => import('@/pages/Login'))
const BigScreen = lazy(() => import('@/pages/BigScreen'))
const Forbidden = lazy(() => import('@/pages/Forbidden'))
const SSEDiagnostic = lazy(() => import('@/pages/SSEDiagnostic'))

const TrendAnalysis = lazy(() => import('@/pages/analytics/TrendAnalysis'))
const FunnelAnalysis = lazy(() => import('@/pages/analytics/FunnelAnalysis'))
const RetentionAnalysis = lazy(() => import('@/pages/analytics/RetentionAnalysis'))
const PathAnalysis = lazy(() => import('@/pages/analytics/PathAnalysis'))
const SegmentationAnalysis = lazy(() => import('@/pages/analytics/SegmentationAnalysis'))
const HeatmapAnalysis = lazy(() => import('@/pages/analytics/HeatmapAnalysis'))

const Users = lazy(() => import('@/pages/admin/Users'))
const Applications = lazy(() => import('@/pages/admin/Applications'))
const Webhooks = lazy(() => import('@/pages/admin/Webhooks'))
const AuditLog = lazy(() => import('@/pages/admin/AuditLog'))

function Loading() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-lg">加载中...</div>
    </div>
  )
}

// 路由守卫包装器
function ProtectedRoute({
  children,
  path,
}: {
  children: React.ReactNode
  path: string
}) {
  return <RouteGuard path={path}>{children}</RouteGuard>
}

export const router = createBrowserRouter([
  {
    path: ROUTES.LOGIN,
    element: (
      <Suspense fallback={<Loading />}>
        <Login />
      </Suspense>
    ),
  },
  {
    path: ROUTES.FORBIDDEN,
    element: (
      <Suspense fallback={<Loading />}>
        <Forbidden />
      </Suspense>
    ),
  },
  {
    path: '/sse-diagnostic',
    element: (
      <Suspense fallback={<Loading />}>
        <SSEDiagnostic />
      </Suspense>
    ),
  },
  {
    path: ROUTES.HOME,
    element: (
      <Suspense fallback={<Loading />}>
        <ProtectedRoute path={ROUTES.HOME}>
          <BigScreen />
        </ProtectedRoute>
      </Suspense>
    ),
  },
  {
    element: <AppLayout />,
    children: [
      {
        path: ROUTES.ANALYTICS.TREND,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.TREND}>
              <TrendAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ANALYTICS.FUNNEL,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.FUNNEL}>
              <FunnelAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ANALYTICS.RETENTION,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.RETENTION}>
              <RetentionAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ANALYTICS.PATH,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.PATH}>
              <PathAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ANALYTICS.SEGMENTATION,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.SEGMENTATION}>
              <SegmentationAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ANALYTICS.HEATMAP,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ANALYTICS.HEATMAP}>
              <HeatmapAnalysis />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ADMIN.USERS,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ADMIN.USERS}>
              <Users />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ADMIN.APPLICATIONS,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ADMIN.APPLICATIONS}>
              <Applications />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ADMIN.WEBHOOKS,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ADMIN.WEBHOOKS}>
              <Webhooks />
            </ProtectedRoute>
          </Suspense>
        ),
      },
      {
        path: ROUTES.ADMIN.AUDIT_LOG,
        element: (
          <Suspense fallback={<Loading />}>
            <ProtectedRoute path={ROUTES.ADMIN.AUDIT_LOG}>
              <AuditLog />
            </ProtectedRoute>
          </Suspense>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to={ROUTES.HOME} replace />,
  },
])
