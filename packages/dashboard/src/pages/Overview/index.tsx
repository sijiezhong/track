import { Suspense, lazy, useEffect, useMemo, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import {
  getOverview,
  getPVUVSeries,
  getPagesTop,
  getEventsDistribution,
  getWebVitals,
} from "@/services/analytics";
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
import OverviewHero from "./OverviewHero";
import ClickableCard from "@/components/common/ClickableCard";
import ChartContainer from "@/components/charts/ChartContainer";
import OverviewUserPathMini from "./OverviewUserPathMini";
const PvUvArea = lazy(() => import("@/components/charts/PvUvArea"));
const EventPie = lazy(() => import("@/components/charts/EventPie"));
const VitalsBars = lazy(() => import("@/components/charts/VitalsBars"));

export default function Overview() {
  const { appId, start, end, refreshKey } = useFiltersStore();
  const [loading, setLoading] = useState(false);
  const [kpi, setKpi] = useState<{
    pv: number;
    uv: number;
    bounceRate: number;
    avgDurationSec: number;
  } | null>(null);
  const [series, setSeries] = useState<
    { ts: string; pv: number; uv: number }[]
  >([]);
  const [pagesTop, setPagesTop] = useState<
    { pageUrl: string; pv: number; uv: number; avgDurationSec: number }[]
  >([]);
  const [dist, setDist] = useState<{ type: string; value: number }[]>([]);
  const [vitals, setVitals] = useState<{
    p50: number;
    p75: number;
    p95: number;
    unit: string;
  } | null>(null);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setKpi(null);
      setSeries([]);
      setPagesTop([]);
      setDist([]);
      setVitals(null);
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);
    Promise.all([
      getOverview({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
      }),
      getPVUVSeries({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
        interval: "hour",
      }),
      getPagesTop({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
        limit: 10,
      }),
      getEventsDistribution({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
      }),
      getWebVitals({
        appId: appId,
        start: start || undefined,
        end: end || undefined,
        metric: "LCP",
      }),
    ])
      .then(([ov, s, p, d, w]) => {
        if (!mounted) return;
        if (ov)
          setKpi({
            pv: ov.pv || 0,
            uv: ov.uv || 0,
            bounceRate: ov.bounceRate || 0,
            avgDurationSec: ov.avgDurationSec || 0,
          });
        setSeries(s?.series || []);
        setPagesTop(p?.list || []);
        setDist(d?.list || []);
        setVitals(w ?? null);
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [appId, start, end, refreshKey]);

  return (
    <div className="space-y-8">
      <OverviewHero />

      {/* 第一行：KPI */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-stretch">
        {["PV", "UV", "跳出率", "平均停留(s)"].map((title, i) => (
          <ClickableCard key={title} to="/analytics/trends" className="h-full">
            <CardHeader className="pb-2">
              <CardTitle className="text-xs text-zinc-500 font-normal">
                {title}
              </CardTitle>
            </CardHeader>
            <CardContent className="min-h-[120px]">
              <div className="text-3xl md:text-4xl font-semibold">
                {loading || !kpi
                  ? "—"
                  : i === 0
                    ? kpi.pv
                    : i === 1
                      ? kpi.uv
                      : i === 2
                        ? `${Math.round(kpi.bounceRate * 100)}%`
                        : Math.round(kpi.avgDurationSec)}
              </div>
            </CardContent>
          </ClickableCard>
        ))}
      </div>

      {/* 第二行：趋势 & WebVitals */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-stretch">
        <ClickableCard to="/analytics/trends" className="h-full">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">PV/UV 趋势</CardTitle>
          </CardHeader>
          <CardContent className="h-full">
            <ChartContainer
              isLoading={loading}
              isEmpty={!loading && (series?.length || 0) === 0}
              minHeight={320}
            >
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <PvUvArea series={series} />
              </Suspense>
            </ChartContainer>
          </CardContent>
        </ClickableCard>
        <ClickableCard to="/analytics/performance" className="h-full">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">Web Vitals 分位</CardTitle>
          </CardHeader>
          <CardContent className="h-full">
            <ChartContainer
              isLoading={loading}
              isEmpty={!loading && !vitals}
              minHeight={320}
            >
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <VitalsBars
                  data={[
                    {
                      metric: "LCP",
                      p50: vitals?.p50 || 0,
                      p75: vitals?.p75 || 0,
                      p95: vitals?.p95 || 0,
                    },
                  ]}
                />
              </Suspense>
            </ChartContainer>
          </CardContent>
        </ClickableCard>
      </div>

      {/* 第三行：TopN & 分布 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-stretch">
        <ClickableCard to="/analytics/pages" className="h-full">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">页面 Top</CardTitle>
          </CardHeader>
          <CardContent className="h-full">
            <div className="overflow-x-auto min-h-[320px]">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>URL</TableHead>
                    <TableHead>PV</TableHead>
                    <TableHead>UV</TableHead>
                    <TableHead>平均停留(s)</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pagesTop.map((r) => (
                    <TableRow key={r.pageUrl}>
                      <TableCell
                        className="max-w-[420px] truncate"
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
        </ClickableCard>
        <ClickableCard to="/events" className="h-full">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">事件类型分布</CardTitle>
          </CardHeader>
          <CardContent className="h-full">
            <ChartContainer
              isLoading={loading}
              isEmpty={!loading && (dist?.length || 0) === 0}
              minHeight={320}
            >
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <EventPie data={dist} />
              </Suspense>
            </ChartContainer>
          </CardContent>
        </ClickableCard>
        <ClickableCard to="/user/behavior" className="h-full">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">用户行为路径（缩略）</CardTitle>
          </CardHeader>
          <CardContent className="h-full">
            <OverviewUserPathMini height={320} />
          </CardContent>
        </ClickableCard>
      </div>
    </div>
  );
}
