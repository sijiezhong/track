import { useEffect, useRef } from "react";
import { Chart } from "@antv/g2";

type SeriesItem = { ts: string; p50: number; p75: number; p95: number };

export default function VitalsArea({
  series,
  metric,
}: {
  series: SeriesItem[] | undefined | null;
  metric: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current) return;
    const chart = new Chart({
      container: ref.current,
      autoFit: true,
      height: 300,
    });
    const safe = Array.isArray(series) ? series : [];
    const data = [
      ...safe.map((d) => ({ ts: d.ts, value: d.p50, quantile: "P50" })),
      ...safe.map((d) => ({ ts: d.ts, value: d.p75, quantile: "P75" })),
      ...safe.map((d) => ({ ts: d.ts, value: d.p95, quantile: "P95" })),
    ];
    chart.data(data);
    chart.scale("ts", { type: "cat" });
    chart
      .line()
      .encode("x", "ts")
      .encode("y", "value")
      .encode("color", "quantile");
    chart.render();
    return () => chart.destroy();
  }, [series, metric]);

  return <div ref={ref} />;
}
