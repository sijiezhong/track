import { useEffect, useRef } from "react";
import { Chart } from "@antv/g2";

type SeriesItem = { ts: string; pv: number; uv: number };

export default function PvUvArea({
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
    });
    const safe = Array.isArray(series) ? series : [];
    const data = [
      ...safe.map((d) => ({ ts: d.ts, value: d.pv, type: "PV" })),
      ...safe.map((d) => ({ ts: d.ts, value: d.uv, type: "UV" })),
    ];
    chart.data(data);
    chart.scale("ts", { type: "cat" });
    chart.line().encode("x", "ts").encode("y", "value").encode("color", "type");
    chart
      .area()
      .encode("x", "ts")
      .encode("y", "value")
      .encode("color", "type")
      .style("fillOpacity", 0.15);
    chart.render();
    return () => chart.destroy();
  }, [series]);

  return <div ref={ref} />;
}
