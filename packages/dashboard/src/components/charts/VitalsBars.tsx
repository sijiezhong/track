import { useEffect, useRef } from "react";
import { Chart } from "@antv/g2";

type Item = { metric: string; p50: number; p75: number; p95: number };

export default function VitalsBars({ data }: { data: Item[] }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!ref.current) return;
    const chart = new Chart({
      container: ref.current,
      autoFit: true,
      height: 300,
    });
    const ds = data
      .map((d) => [
        { metric: d.metric, quantile: "P50", value: d.p50 },
        { metric: d.metric, quantile: "P75", value: d.p75 },
        { metric: d.metric, quantile: "P95", value: d.p95 },
      ])
      .flat();
    chart.data(ds);
    chart
      .interval()
      .encode("x", "metric")
      .encode("y", "value")
      .encode("color", "quantile")
      .transform({ type: "dodgeX" });
    chart.render();
    return () => chart.destroy();
  }, [data]);
  return <div ref={ref} />;
}
