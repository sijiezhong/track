import { useEffect, useMemo, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getPagesTop } from "@/services/analytics";
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
import Container from "@/components/layout/Container";
import PageIntro from "@/components/common/PageIntro";

export default function AnalyticsPages() {
  const { appId, start, end } = useFiltersStore();
  const [keyword, setKeyword] = useState("");
  const [rows, setRows] = useState<
    { pageUrl: string; pv: number; uv: number; avgDurationSec: number }[]
  >([]);
  const [dense, setDense] = useState(false);
  const canQuery = useMemo(() => appId && appId.length > 0, [appId]);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setRows([]);
      return;
    }

    let mounted = true;
    getPagesTop({
      appId: appId,
      start: start || undefined,
      end: end || undefined,
      limit: 100,
    }).then((res) => {
      if (!mounted) return;
      setRows(res.list || []);
    });
    return () => {
      mounted = false;
    };
  }, [appId, start, end]);

  const safeRows = Array.isArray(rows) ? rows : [];
  const filtered = safeRows.filter((r) =>
    (r.pageUrl || "").toLowerCase().includes(keyword.toLowerCase()),
  );

  const exportCSV = () => {
    const header = ["URL", "PV", "UV", "平均停留(s)"];
    const lines = filtered.map((r) =>
      [r.pageUrl, r.pv, r.uv, r.avgDurationSec].join(","),
    );
    const csv = [header.join(","), ...lines].join("\n");
    const blob = new Blob(["\ufeff" + csv], {
      type: "text/csv;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "pages.csv";
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">页面分析</h2>
        <PageIntro
          storageKey="analytics-pages"
          title="本页内容"
          description={
            <p>
              统计页面访问 Top 列表，包含 PV、UV 与平均停留时长。可按 URL
              关键字过滤与导出 CSV，用于定位高流量页面、跳出高或停留低的页面。
            </p>
          }
          terms={[
            {
              term: "PV",
              definition: "Page View，页面被浏览的次数（可重复）。",
            },
            {
              term: "UV",
              definition: "Unique Visitor，独立访客数（去重的用户）。",
            },
            {
              term: "平均停留时长",
              definition: "用户在页面上停留的平均秒数。",
            },
          ]}
        />
        {
          <>
            <div className="sticky top-0 z-30 -mt-4 pt-4 pb-3 bg-background/80 backdrop-blur border-b flex items-center gap-2 justify-between">
              <div className="flex items-center gap-2">
                <Input
                  placeholder="按 URL 关键字过滤"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  className="w-[300px]"
                />
              </div>
              <div className="flex items-center gap-2">
                <Button
                  aria-label="切换表格密度"
                  variant="outline"
                  size="sm"
                  onClick={() => setDense((d) => !d)}
                >
                  {dense ? "舒适密度" : "紧凑密度"}
                </Button>
                <Button
                  aria-label="导出CSV"
                  variant="outline"
                  size="sm"
                  onClick={exportCSV}
                >
                  导出 CSV
                </Button>
              </div>
            </div>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">页面 Top</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-auto">
                  <Table>
                    <TableHeader className="sticky top-0 bg-background">
                      <TableRow>
                        <TableHead className="sticky left-0 bg-background z-10">
                          URL
                        </TableHead>
                        <TableHead>PV</TableHead>
                        <TableHead>UV</TableHead>
                        <TableHead>平均停留(s)</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {filtered.map((r) => (
                        <TableRow
                          key={r.pageUrl}
                          className={dense ? "h-9" : "h-12"}
                        >
                          <TableCell
                            className="max-w-[520px] truncate sticky left-0 bg-background z-10"
                            title={r.pageUrl}
                          >
                            {r.pageUrl}
                          </TableCell>
                          <TableCell>{r.pv}</TableCell>
                          <TableCell>{r.uv}</TableCell>
                          <TableCell>{r.avgDurationSec}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </CardContent>
            </Card>
          </>
        }
      </div>
    </Container>
  );
}
