import { Suspense, lazy, useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getWebVitals, getWebVitalsSeries } from "@/services/analytics";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
const VitalsBars = lazy(() => import("@/components/charts/VitalsBars"));
const VitalsArea = lazy(() => import("@/components/charts/VitalsArea"));
import Container from "@/components/layout/Container";
import PageIntro from "@/components/common/PageIntro";

const METRICS = [
  { value: "LCP", label: "LCP (Largest Contentful Paint)" },
  { value: "FID", label: "FID (First Input Delay)" },
  { value: "CLS", label: "CLS (Cumulative Layout Shift)" },
  { value: "FCP", label: "FCP (First Contentful Paint)" },
  { value: "TTFB", label: "TTFB (Time to First Byte)" },
];

export default function AnalyticsPerformance() {
  const { appId, start, end, refreshKey } = useFiltersStore();
  const [loading, setLoading] = useState(false);
  const [selectedMetric, setSelectedMetric] = useState("LCP");
  const [vitalsData, setVitalsData] = useState<{
    p50: number;
    p75: number;
    p95: number;
    unit: string;
  } | null>(null);
  const [series, setSeries] = useState<
    { ts: string; p50: number; p75: number; p95: number }[]
  >([]);
  const [allMetricsVitals, setAllMetricsVitals] = useState<
    { metric: string; p50: number; p75: number; p95: number }[]
  >([]);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setVitalsData(null);
      setSeries([]);
      setAllMetricsVitals([]);
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);

    // 获取所有指标的分位数据用于对比
    Promise.all(
      METRICS.map((m) =>
        getWebVitals({
          appId: appId,
          start: start || undefined,
          end: end || undefined,
          metric: m.value,
        }),
      ),
    )
      .then((results) => {
        if (!mounted) return;
        const allVitals = results.map((v, i) => ({
          metric: METRICS[i].value,
          p50: v?.p50 || 0,
          p75: v?.p75 || 0,
          p95: v?.p95 || 0,
        }));
        setAllMetricsVitals(allVitals);
      })
      .catch(() => {});

    // 获取选中指标的分位数据和趋势
    Promise.all([
      getWebVitals({
        appId: appId,
        start: start || "",
        end: end || "",
        metric: selectedMetric,
      }),
      getWebVitalsSeries({
        appId: appId,
        start: start || "",
        end: end || "",
        metric: selectedMetric,
        interval: "hour",
      }),
    ])
      .then(([vitalsRes, seriesRes]) => {
        if (!mounted) return;
        setVitalsData(vitalsRes ?? null);
        setSeries(seriesRes?.series || []);
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [appId, start, end, selectedMetric, refreshKey]);

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">性能分析</h2>
        <PageIntro
          storageKey="analytics-performance"
          title="本页内容"
          description={
            <p>
              关注 Web Vitals（如
              LCP、FID、CLS、FCP、TTFB）等核心性能指标的分位值与趋势，支持选择指标对比与时序分析，用于定位性能退化与优化优先级。
            </p>
          }
          terms={[
            {
              term: "LCP",
              definition:
                "Largest Contentful Paint，最大内容绘制时间，建议 ≤ 2.5s。",
            },
            {
              term: "FID",
              definition:
                "First Input Delay，首次输入延迟，建议 ≤ 100ms（已被 INP 替代趋势）。",
            },
            {
              term: "CLS",
              definition: "Cumulative Layout Shift，累计布局偏移，建议 ≤ 0.1。",
            },
            {
              term: "FCP",
              definition: "First Contentful Paint，首次内容绘制时间。",
            },
            {
              term: "TTFB",
              definition: "Time To First Byte，首字节时间，衡量后端/网络响应。",
            },
            {
              term: "P50/P75/P95",
              definition: "分位数：50%/75%/95% 的用户体验不超过该值。",
            },
          ]}
        />

        {/* 第一行：指标选择器和所有指标对比 */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs text-zinc-500 font-normal">
                选择指标
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Select value={selectedMetric} onValueChange={setSelectedMetric}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {METRICS.map((m) => (
                    <SelectItem key={m.value} value={m.value}>
                      {m.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </CardContent>
          </Card>
          <Card className="lg:col-span-2">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">所有指标分位对比</CardTitle>
            </CardHeader>
            <CardContent>
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <VitalsBars data={allMetricsVitals} />
              </Suspense>
            </CardContent>
          </Card>
        </div>

        {/* 第二行：当前指标的分位数据和趋势 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">{selectedMetric} 分位值</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-[320px]" />
              ) : vitalsData ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-zinc-500">P50</span>
                    <span className="text-lg font-semibold">
                      {vitalsData.p50} {vitalsData.unit}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-zinc-500">P75</span>
                    <span className="text-lg font-semibold">
                      {vitalsData.p75} {vitalsData.unit}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-zinc-500">P95</span>
                    <span className="text-lg font-semibold">
                      {vitalsData.p95} {vitalsData.unit}
                    </span>
                  </div>
                  <Suspense fallback={<Skeleton className="h-[200px]" />}>
                    <VitalsBars
                      data={[
                        {
                          metric: selectedMetric,
                          p50: vitalsData.p50,
                          p75: vitalsData.p75,
                          p95: vitalsData.p95,
                        },
                      ]}
                    />
                  </Suspense>
                </div>
              ) : (
                <div className="text-center text-zinc-500 py-8">暂无数据</div>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">{selectedMetric} 趋势</CardTitle>
            </CardHeader>
            <CardContent>
              <Suspense fallback={<Skeleton className="h-[320px]" />}>
                <VitalsArea series={series} metric={selectedMetric} />
              </Suspense>
            </CardContent>
          </Card>
        </div>
      </div>
    </Container>
  );
}
