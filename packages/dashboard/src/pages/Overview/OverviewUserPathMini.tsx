import { useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getEvents } from "@/services/events";
import UserPathGraph, {
  type GraphData,
} from "@/components/graphs/UserPathGraph";
import ChartContainer from "@/components/charts/ChartContainer";

export default function OverviewUserPathMini({
  height = 240,
}: {
  height?: number;
}) {
  const { appId, start, end } = useFiltersStore();
  const [graph, setGraph] = useState<GraphData>({ nodes: [], edges: [] });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setGraph({ nodes: [], edges: [] });
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);
    getEvents({
      appId: appId,
      start: start || undefined,
      end: end || undefined,
      page: 1,
      size: 50,
    })
      .then((res) => {
        if (!mounted) return;
        const items = res?.items || [];
        const seq = items
          .map((e: any) => e?.pageUrl as string)
          .filter(Boolean)
          .slice(0, 20);

        const validUrls: string[] = [];
        seq.forEach((url) => {
          try {
            const urlObj = new URL(url);
            validUrls.push(urlObj.pathname || urlObj.href);
          } catch {
            if (url) validUrls.push(url);
          }
        });

        if (validUrls.length === 0) {
          setGraph({ nodes: [], edges: [] });
          return;
        }

        const uniq = Array.from(new Set(validUrls));
        const nodes = uniq.slice(0, 12).map((u, i) => {
          const id = String(i);
          const label = u.length > 30 ? u.substring(0, 30) + "..." : u;
          return { id, label };
        });

        const edges: { source: string; target: string }[] = [];
        for (let i = 1; i < validUrls.length; i++) {
          const a = uniq.indexOf(validUrls[i - 1]);
          const b = uniq.indexOf(validUrls[i]);
          if (a !== -1 && b !== -1 && a !== b && a < 12 && b < 12) {
            edges.push({ source: String(a), target: String(b) });
          }
        }

        setGraph({ nodes, edges });
      })
      .catch(() => setGraph({ nodes: [], edges: [] }))
      .finally(() => mounted && setLoading(false));

    return () => {
      mounted = false;
    };
  }, [appId, start, end]);

  const isEmpty = !graph?.nodes?.length || !graph?.edges?.length;

  return (
    <ChartContainer
      isLoading={loading}
      isEmpty={!loading && isEmpty}
      minHeight={height}
    >
      {!loading && !isEmpty && (
        <div className="-m-2">
          <UserPathGraph data={graph} height={height} interactive={false} />
        </div>
      )}
    </ChartContainer>
  );
}
