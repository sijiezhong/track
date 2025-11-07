import { Suspense, lazy, useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getCustomEvents, getCustomEventsTop } from "@/services/analytics";
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

export default function AnalyticsCustom() {
  const { appId, start, end, refreshKey } = useFiltersStore();
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [series, setSeries] = useState<{ ts: string; count: number }[]>([]);
  const [topList, setTopList] = useState<{ eventId: string; count: number }[]>(
    [],
  );
  const [total, setTotal] = useState(0);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setSeries([]);
      setTotal(0);
      setTopList([]);
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);
    Promise.all([
      getCustomEvents({
        appId: appId || undefined,
        start: start || undefined,
        end: end || undefined,
        groupBy: "hour",
      }),
      getCustomEventsTop({
        appId: appId || undefined,
        start: start || undefined,
        end: end || undefined,
        limit: 100,
      }),
    ])
      .then(([trendRes, topRes]) => {
        if (!mounted) return;
        setSeries(trendRes?.series || []);
        setTotal(trendRes?.total || 0);
        setTopList(topRes?.list || []);
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [appId, start, end, refreshKey]);

  const filteredList = topList.filter((item) =>
    keyword ? item.eventId.toLowerCase().includes(keyword.toLowerCase()) : true,
  );

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">自定义事件分析</h2>
        <PageIntro
          storageKey="analytics-custom"
          title="本页内容"
          description={
            <p>
              分析自定义埋点事件的总量、趋势与 Top
              列表，帮助评估关键业务行为（如注册、下单、点击等）的触发情况与变化趋势。
            </p>
          }
        />

        {/* 第一行：总览和趋势 */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs text-zinc-500 font-normal">
                事件总数
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {loading ? "—" : total}
              </div>
            </CardContent>
          </Card>
          <Card className="lg:col-span-2">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">事件趋势</CardTitle>
            </CardHeader>
            <CardContent>
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <CountArea series={series} />
              </Suspense>
            </CardContent>
          </Card>
        </div>

        {/* 第二行：事件列表 */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">事件 Top</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mb-4">
              <Input
                placeholder="按 eventId 过滤"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="w-[300px]"
              />
            </div>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>事件ID</TableHead>
                    <TableHead>触发次数</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredList.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={2}
                        className="text-center text-zinc-500"
                      >
                        {loading ? "加载中..." : "暂无数据"}
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredList.map((item, idx) => (
                      <TableRow key={`${item.eventId}-${idx}`}>
                        <TableCell className="font-mono">
                          {item.eventId}
                        </TableCell>
                        <TableCell>{item.count}</TableCell>
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
