import { Suspense, lazy, useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getErrorsTrend, getErrorsTop } from "@/services/analytics";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Input } from "@/components/ui/input";
const CountArea = lazy(() => import("@/components/charts/CountArea"));
import Container from "@/components/layout/Container";
import PageIntro from "@/components/common/PageIntro";

export default function AnalyticsErrors() {
  const { appId, start, end, refreshKey } = useFiltersStore();
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [series, setSeries] = useState<{ ts: string; count: number }[]>([]);
  const [topList, setTopList] = useState<
    {
      fingerprint: string;
      message: string;
      count: number;
      firstSeen: string;
      lastSeen: string;
    }[]
  >([]);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setSeries([]);
      setTopList([]);
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);
    Promise.all([
      getErrorsTrend({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
        interval: "hour",
      }),
      getErrorsTop({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
        limit: 50,
      }),
    ])
      .then(([trendRes, topRes]) => {
        if (!mounted) return;
        setSeries(trendRes?.series || []);
        setTopList(topRes?.list || []);
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [appId, start, end, refreshKey]);

  const filteredList = topList.filter(
    (item) =>
      !keyword ||
      item.message.toLowerCase().includes(keyword.toLowerCase()) ||
      item.fingerprint.toLowerCase().includes(keyword.toLowerCase()),
  );

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">错误分析</h2>
        <PageIntro
          storageKey="analytics-errors"
          title="本页内容"
          description={
            <p>
              统计前端运行时错误与资源加载错误的趋势和 Top
              聚类（按指纹归因），支持关键词过滤，用于快速定位高频错误与回归问题。
            </p>
          }
        />

        {/* 第一行：错误趋势 */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">错误趋势</CardTitle>
          </CardHeader>
          <CardContent>
            <Suspense fallback={<Skeleton className="h-[320px]" />}>
              <CountArea series={series} />
            </Suspense>
          </CardContent>
        </Card>

        {/* 第二行：Top 错误列表 */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">Top 错误</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mb-4">
              <Input
                placeholder="按错误消息或指纹过滤"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="w-[300px]"
              />
            </div>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>错误消息</TableHead>
                    <TableHead>指纹</TableHead>
                    <TableHead>次数</TableHead>
                    <TableHead>首次出现</TableHead>
                    <TableHead>最后出现</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredList.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={5}
                        className="text-center text-zinc-500"
                      >
                        {loading ? "加载中..." : "暂无数据"}
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredList.map((item) => (
                      <TableRow key={item.fingerprint}>
                        <TableCell
                          className="max-w-[400px] truncate"
                          title={item.message}
                        >
                          {item.message}
                        </TableCell>
                        <TableCell className="font-mono text-xs">
                          {item.fingerprint}
                        </TableCell>
                        <TableCell>{item.count}</TableCell>
                        <TableCell className="text-xs">
                          {item.firstSeen}
                        </TableCell>
                        <TableCell className="text-xs">
                          {item.lastSeen}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </div>
    </Container>
  );
}
