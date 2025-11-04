import { useEffect, useRef } from "react";
import { Graph } from "@antv/g6";

export type GraphData = {
  nodes: Array<{ id: string; label: string }>;
  edges: Array<{ source: string; target: string }>;
};

export default function UserPathGraph({
  data,
  height = 600,
  interactive = true,
}: {
  data: GraphData;
  height?: number;
  interactive?: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current) return;

    // 处理空数据或无效数据
    if (!data?.nodes?.length || !data?.edges?.length) {
      return;
    }

    // 验证数据：确保所有节点都有有效的 ID，所有边的 source/target 都指向有效节点
    const nodeIds = new Set(data.nodes.map((n) => n.id).filter(Boolean));
    const validEdges = data.edges.filter((e) => {
      return (
        e.source && e.target && nodeIds.has(e.source) && nodeIds.has(e.target)
      );
    });

    if (validEdges.length === 0) {
      return;
    }

    const validNodes = data.nodes.filter((n) => n.id && nodeIds.has(n.id));
    const nodeMap = new Map(validNodes.map((n) => [n.id, n]));

    const graph = new Graph({
      container: ref.current,
      width: ref.current.clientWidth,
      height,
      data: {
        nodes: validNodes,
        edges: validEdges,
      },
      layout: {
        type: "force",
        preventOverlap: true,
        nodeSize: 60,
        nodeSpacing: 30,
        animation: true,
      },
      node: {
        style: {
          size: 40,
          fill: "#1890ff",
          stroke: "#096dd9",
          lineWidth: 2,
          labelText: (d: any) => d.label,
          labelFill: "#000",
          labelFontSize: 12,
          labelWordWrap: true,
          labelWordWrapWidth: 100,
        },
        state: {
          hover: {
            fill: "#40a9ff",
            stroke: "#1890ff",
            lineWidth: 3,
          },
        },
      },
      edge: {
        style: {
          stroke: "#91d5ff",
          lineWidth: 2,
          endArrow: true,
          endArrowSize: 8,
        },
        state: {
          hover: {
            stroke: "#40a9ff",
            lineWidth: 3,
          },
        },
      },
      behaviors: interactive ? ["zoom-canvas"] : [],
      plugins: [
        {
          type: "tooltip",
          getContent: (evt: any) => {
            const { itemType, itemId } = evt;
            if (itemType === "node" && itemId) {
              const node = nodeMap.get(itemId);
              return node?.label
                ? `<div style="padding: 8px;"><strong>${node.label}</strong></div>`
                : "";
            }
            return "";
          },
        },
      ],
    });

    // 添加节点悬停效果
    graph.on("node:pointerenter", (evt: any) => {
      if (evt.itemId) {
        graph.setElementState(evt.itemId, "hover", true);
      }
    });

    graph.on("node:pointerleave", (evt: any) => {
      if (evt.itemId) {
        graph.setElementState(evt.itemId, "hover", false);
      }
    });

    // 添加边悬停效果
    graph.on("edge:pointerenter", (evt: any) => {
      if (evt.itemId) {
        graph.setElementState(evt.itemId, "hover", true);
      }
    });

    graph.on("edge:pointerleave", (evt: any) => {
      if (evt.itemId) {
        graph.setElementState(evt.itemId, "hover", false);
      }
    });

    graph.render();

    // 等待布局完成后，自适应视图显示全部内容
    graph.on("afterlayout", () => {
      // 使用 setTimeout 确保布局完全完成后再调用 fitView
      setTimeout(() => {
        try {
          // G6 v5 的 fitView 方法
          graph.fitView();
        } catch (e) {
          console.warn("fitView failed:", e);
        }
      }, 100);
    });

    return () => {
      graph.destroy();
    };
  }, [data]);

  // 处理空数据情况
  if (!data?.nodes?.length || !data?.edges?.length) {
    return (
      <div
        className="flex items-center justify-center text-gray-400"
        style={{ height }}
      >
        暂无数据
      </div>
    );
  }

  return <div ref={ref} className="w-full" style={{ height }} />;
}
