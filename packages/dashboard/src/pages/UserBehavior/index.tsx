import { useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getEvents } from "@/services/events";
import UserPathGraph, {
  type GraphData,
} from "@/components/graphs/UserPathGraph";
import Container from "@/components/layout/Container";
import { Button } from "@/components/ui/button";
import PageIntro from "@/components/common/PageIntro";

export default function UserBehavior() {
  const { appId, start, end, refreshKey } = useFiltersStore();
  const [graph, setGraph] = useState<GraphData>({ nodes: [], edges: [] });
  const [graphKey, setGraphKey] = useState(0);

  useEffect(() => {
    // 如果未选择项目，不调用接口
    if (!appId) {
      setGraph({ nodes: [], edges: [] });
      return;
    }

    let mounted = true;
    getEvents({
      appId: appId || undefined,
      start: start || undefined,
      end: end || undefined,
      page: 1,
      size: 50,
    })
      .then((res) => {
        if (!mounted) return;
        // 简单事件序列 → 路径图：按 pageUrl 顺序连线
        const items = res?.items || [];
        const seq = items
          .map((e) => e?.pageUrl as string)
          .filter(Boolean)
          .slice(0, 20);

        // 验证并处理 URL，提取有效的路径
        const validUrls: string[] = [];
        seq.forEach((url) => {
          try {
            const urlObj = new URL(url);
            validUrls.push(urlObj.pathname || urlObj.href);
          } catch {
            // 如果 URL 无效，尝试使用原始值或跳过
            if (url) validUrls.push(url);
          }
        });

        if (validUrls.length === 0) {
          setGraph({ nodes: [], edges: [] });
          return;
        }

        const uniq = Array.from(new Set(validUrls));
        const nodes = uniq.map((u, i) => {
          const id = String(i);
          const label = u.length > 30 ? u.substring(0, 30) + "..." : u;
          return { id, label };
        });

        // 使用 Set 去重边，避免重复边导致 G6 报错
        const edgeSet = new Set<string>();
        const edges: { source: string; target: string }[] = [];
        for (let i = 1; i < validUrls.length; i++) {
          const a = uniq.indexOf(validUrls[i - 1]);
          const b = uniq.indexOf(validUrls[i]);
          if (a !== -1 && b !== -1 && a !== b) {
            const edgeKey = `${a}-${b}`;
            if (!edgeSet.has(edgeKey)) {
              edgeSet.add(edgeKey);
              edges.push({ source: String(a), target: String(b) });
            }
          }
        }

        setGraph({ nodes, edges });
      })
      .catch((err) => {
        console.error("Failed to load events:", err);
        if (!mounted) return;
        setGraph({ nodes: [], edges: [] });
      });
    return () => {
      mounted = false;
    };
  }, [appId, start, end, refreshKey]);

  return (
    <Container>
      <div className="space-y-6">
        <h2 className="text-lg font-semibold">用户行为路径</h2>
        <PageIntro
          storageKey="user-behavior"
          title="本页内容"
          description={
            <p>
              基于用户访问顺序构建路径图，展示常见页面跳转路径与分叉，帮助识别关键入口/流失节点与优化旅程。
            </p>
          }
        />
        <div className="sticky top-0 z-30 -mt-4 pt-4 pb-3 bg-background/80 backdrop-blur border-b flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setGraphKey((k) => k + 1)}
          >
            视图重置
          </Button>
        </div>
        <div className="rounded border p-4">
          <UserPathGraph key={graphKey} data={graph} />
        </div>
      </div>
    </Container>
  );
}
