import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Search } from 'lucide-react'

export default function AuditLog() {
  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">操作日志</h1>
        <p className="text-muted-foreground mt-2">查看系统操作审计日志</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置查询条件</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label htmlFor="username">操作用户</Label>
              <Input id="username" placeholder="请输入用户名" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="action">操作类型</Label>
              <Input id="action" placeholder="如: CREATE, UPDATE" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="startTime">开始时间</Label>
              <Input id="startTime" type="datetime-local" />
            </div>
            <div className="flex items-end">
              <Button className="w-full">
                <Search className="mr-2 h-4 w-4" />
                查询
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>操作日志</CardTitle>
          <CardDescription>暂无日志记录</CardDescription>
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