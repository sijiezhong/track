import { Suspense, lazy, useEffect, useMemo, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getPVUVSeries } from "@/services/analytics";
import { Skeleton } from "@/components/ui/skeleton";
const PvUvArea = lazy(() => import("@/components/charts/PvUvArea"));
import Container from "@/components/layout/Container";
import PageIntro from "@/components/common/PageIntro";

export default function AnalyticsTrends() {
  const { appId, start, end } = useFiltersStore();
  const [series, setSeries] = useState<
    { ts: string; pv: number; uv: number }[]
  >([]);

  useEffect(() => {
    let mounted = true;
    getPVUVSeries({
      appId: appId || undefined,
      start: start || "",
      end: end || "",
      interval: "hour",
    }).then((res) => {
      if (!mounted) return;
      setSeries(res.series);
    });
    return () => {
      mounted = false;
    };
  }, [appId, start, end]);

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">趋势分析</h2>
        <PageIntro
          storageKey="analytics-trends"
          title="本页内容"
          description={
            <p>
              展示 PV/UV
              随时间的变化趋势，可按应用、时间范围与是否包含爬虫过滤，用于识别流量峰谷与异常波动。
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
          ]}
        />
        <div className="rounded border p-4">
          <Suspense fallback={<Skeleton className="h-[320px]" />}>
            <PvUvArea series={series} />
          </Suspense>
        </div>
      </div>
    </Container>
  );
}
