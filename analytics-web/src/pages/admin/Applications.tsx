import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { adminApi, type Application } from '@/services/admin'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Loader2, Plus } from 'lucide-react'
import { formatDate } from '@/lib/format'

export default function Applications() {
  const { appId } = useAuth()
  const [apps, setApps] = useState<Application[]>([])
  const [loading, setLoading] = useState(false)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [formData, setFormData] = useState({
    appName: '',
    appKey: '',
    description: '',
  })

  const fetchApps = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await adminApi.getApplications(appId)
      setApps(response.data)
    } catch (error) {
      console.error('Failed to fetch applications:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async () => {
    if (!appId) return

    setLoading(true)
    try {
      await adminApi.createApplication(appId, formData)
      setShowCreateForm(false)
      setFormData({ appName: '', appKey: '', description: '' })
      await fetchApps()
    } catch (error) {
      console.error('Failed to create application:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchApps()
  }, [appId])

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">应用管理</h1>
          <p className="text-muted-foreground mt-2">管理埋点应用配置</p>
        </div>
        <Button onClick={() => setShowCreateForm(!showCreateForm)}>
          <Plus className="mr-2 h-4 w-4" />
          创建应用
        </Button>
      </div>

      {showCreateForm && (
        <Card>
          <CardHeader>
            <CardTitle>创建新应用</CardTitle>
            <CardDescription>填写应用信息</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="appName">应用名称</Label>
                <Input
                  id="appName"
                  value={formData.appName}
                  onChange={(e) => setFormData({ ...formData, appName: e.target.value })}
                  placeholder="请输入应用名称"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="appKey">应用密钥</Label>
                <Input
                  id="appKey"
                  value={formData.appKey}
                  onChange={(e) => setFormData({ ...formData, appKey: e.target.value })}
                  placeholder="留空自动生成"
                />
              </div>
              <div className="col-span-2 space-y-2">
                <Label htmlFor="description">描述</Label>
                <Input
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="请输入应用描述"
                />
              </div>
              <div className="col-span-2 flex justify-end gap-2">
                <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                  取消
                </Button>
                <Button onClick={handleCreate} disabled={loading || !formData.appName}>
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      创建中...
                    </>
                  ) : (
                    '创建'
                  )}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>应用列表</CardTitle>
          <CardDescription>共 {apps.length} 个应用</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex h-64 items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : apps.length === 0 ? (
            <div className="flex h-64 items-center justify-center text-muted-foreground">
              暂无应用数据
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>应用名称</TableHead>
                  <TableHead>应用密钥</TableHead>
                  <TableHead>描述</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {apps.map((app) => (
                  <TableRow key={app.id}>
                    <TableCell>{app.id}</TableCell>
                    <TableCell className="font-medium">{app.appName}</TableCell>
                    <TableCell className="font-mono text-xs">{app.appKey}</TableCell>
                    <TableCell className="text-muted-foreground">{app.description || '-'}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDate(app.createTime, 'yyyy-MM-dd HH:mm')}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}