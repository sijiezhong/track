import { useEffect, useRef } from "react";
import { Chart } from "@antv/g2";

type SeriesItem = { ts: string; count: number };

export default function CountArea({
  series,
}: {
  series: SeriesItem[] | undefined | null;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current) return;
    const chart = new Chart({
      container: ref.current,
      autoFit: true,
      height: 300,
      padding: [16, 24, 40, 48],
    });
    const safe = Array.isArray(series) ? series : [];
    chart.data(safe);
    chart.scale("ts", { type: "cat" });
    chart.line().encode("x", "ts").encode("y", "count");
    chart
      .area()
      .encode("x", "ts")
      .encode("y", "count")
      .style("fillOpacity", 0.15);
    chart.render();
    return () => chart.destroy();
  }, [series]);

  return <div ref={ref} />;
}
