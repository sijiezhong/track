import { useEffect, useMemo, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getEvents } from "@/services/events";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import Container from "@/components/layout/Container";
import { toast } from "sonner";
import PageIntro from "@/components/common/PageIntro";

export default function Events() {
  const { appId, start, end } = useFiltersStore();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState("");
  const [rows, setRows] = useState<Record<string, any>[]>([]);
  const [total, setTotal] = useState(0);
  const canQuery = useMemo(() => appId && appId.length > 0, [appId]);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setRows([]);
      setTotal(0);
      return;
    }

    let mounted = true;
    getEvents({
      appId: appId,
      start: start || undefined,
      end: end || undefined,
      keyword,
      page,
      size,
    }).then((res) => {
      if (!mounted) return;
      setRows(res.items || []);
      setTotal(res.page?.total || 0);
    });
    return () => {
      mounted = false;
    };
  }, [appId, start, end, keyword, page, size]);

  const totalPages = Math.max(1, Math.ceil(total / size));
  const copyId = (id?: string) => {
    if (!id) return;
    const text = String(id);
    const onOk = () => toast.success("复制成功", { description: text });
    const onFail = () => toast.error("复制失败，请手动复制");
    try {
      if (navigator.clipboard?.writeText) {
        navigator.clipboard.writeText(text).then(onOk).catch(onFail);
      } else {
        const ta = document.createElement("textarea");
        ta.value = text;
        ta.style.position = "fixed";
        ta.style.left = "-9999px";
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        const ok = document.execCommand("copy");
        document.body.removeChild(ta);
        ok ? onOk() : onFail();
      }
    } catch {
      onFail();
    }
  };

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">事件明细</h2>
        <PageIntro
          storageKey="events"
          title="本页内容"
          description={
            <p>
              查看原始埋点事件的明细与分页，支持关键词检索与每页条数切换，用于数据核对、抽样排查与回溯分析。
            </p>
          }
        />
        {
          <>
            <div className="sticky top-0 z-30 -mt-4 pt-4 pb-3 bg-background/80 backdrop-blur border-b flex items-center gap-3 justify-between">
              <div className="flex items-center gap-2">
                <Input
                  placeholder="关键词（URL、eventId 等）"
                  value={keyword}
                  onChange={(e) => {
                    setKeyword(e.target.value);
                    setPage(1);
                  }}
                  className="w-[300px]"
                />
              </div>
              <div className="flex items-center gap-2">
                <Select
                  value={String(size)}
                  onValueChange={(v) => {
                    setSize(Number(v));
                    setPage(1);
                  }}
                >
                  <SelectTrigger className="w-[120px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[10, 20, 50, 100].map((s) => (
                      <SelectItem key={s} value={String(s)}>
                        {s}/页
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">事件列表</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader className="sticky top-0 bg-background">
                      <TableRow>
                        <TableHead>时间</TableHead>
                        <TableHead>类型</TableHead>
                        <TableHead>URL</TableHead>
                        <TableHead>用户</TableHead>
                        <TableHead>事件ID</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {(Array.isArray(rows) ? rows : []).map((r, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{r.ts}</TableCell>
                          <TableCell>
                            <span className="inline-flex items-center rounded px-2 py-0.5 text-xs border bg-muted/30">
                              {r.type}
                            </span>
                          </TableCell>
                          <TableCell
                            className="max-w-[420px] truncate"
                            title={r.pageUrl}
                          >
                            {r.pageUrl}
                          </TableCell>
                          <TableCell>{r.userId}</TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2">
                              <span
                                className="truncate max-w-[200px]"
                                title={r.customEventId || "-"}
                              >
                                {r.customEventId || "-"}
                              </span>
                              {r.customEventId && (
                                <Button
                                  aria-label="复制事件ID"
                                  variant="outline"
                                  size="sm"
                                  onClick={() => copyId(r.customEventId)}
                                >
                                  复制
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="mt-3 flex items-center justify-between text-sm">
                  <div>共 {total} 条</div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page <= 1}
                      onClick={() => setPage((p) => p - 1)}
                    >
                      上一页
                    </Button>
                    <span>
                      {page} / {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page >= totalPages}
                      onClick={() => setPage((p) => p + 1)}
                    >
                      下一页
                    </Button>
                    <Select
                      value={String(size)}
                      onValueChange={(v) => {
                        setSize(Number(v));
                        setPage(1);
                      }}
                    >
                      <SelectTrigger className="w-[120px]">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {[10, 20, 50, 100].map((s) => (
                          <SelectItem key={s} value={String(s)}>
                            {s}/页
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        }
      </div>
    </Container>
  );
}
