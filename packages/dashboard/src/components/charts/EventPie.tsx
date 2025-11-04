import { useEffect, useRef } from "react";
import { Chart } from "@antv/g2";

type Item = { type: string; value: number };

export default function EventPie({ data }: { data: Item[] }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!ref.current) return;
    const chart = new Chart({
      container: ref.current,
      autoFit: true,
      height: 300,
    });
    chart.coordinate({ type: "theta", outerRadius: 0.8 });
    chart
      .interval()
      .data(data)
      .transform({ type: "stackY" })
      .encode("y", "value")
      .encode("color", "type")
      .label({ text: "type", position: "outside" })
      .tooltip({ items: [{ channel: "y", valueFormatter: (v) => String(v) }] });
    chart.legend(true);
    chart.render();
    return () => chart.destroy();
  }, [data]);
  return <div ref={ref} />;
}
