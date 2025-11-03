import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'

export default function Webhooks() {
  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Webhook 管理</h1>
          <p className="text-muted-foreground mt-2">管理事件推送 Webhook 配置</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          创建 Webhook
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Webhook 列表</CardTitle>
          <CardDescription>暂无 Webhook 配置</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex h-64 items-center justify-center text-muted-foreground">
            功能开发中，敬请期待
          </div>
        </CardContent>
      </Card>
    </div>
  )
}