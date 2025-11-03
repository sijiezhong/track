import { useEffect, useRef } from 'react'
import { Graph } from '@antv/g6'
import type { PathEdge } from '@/types/analytics'

interface PathGraphProps {
  data: PathEdge[]
  height?: number
}

export function PathGraph({ data, height = 500 }: PathGraphProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const graphRef = useRef<Graph | null>(null)

  useEffect(() => {
    if (!containerRef.current || data.length === 0) return

    // 清除之前的图
    if (graphRef.current) {
      graphRef.current.destroy()
    }

    // 提取节点
    const nodeSet = new Set<string>()
    data.forEach((edge) => {
      nodeSet.add(edge.from)
      nodeSet.add(edge.to)
    })

    const nodes = Array.from(nodeSet).map((id) => ({
      id,
      data: { label: id },
    }))

    const edges = data.map((edge, index) => ({
      id: `edge-${index}`,
      source: edge.from,
      target: edge.to,
      data: { weight: edge.count },
    }))

    // 创建图
    const graph = new Graph({
      container: containerRef.current,
      width: containerRef.current.offsetWidth,
      height,
      data: { nodes, edges },
      layout: {
        type: 'force',
      },
      node: {
        style: {
          labelText: (d: any) => d.data.label,
          labelPlacement: 'center',
          size: 40,
          fill: '#3b82f6',
          stroke: '#1e40af',
          lineWidth: 2,
        },
      },
      edge: {
        style: {
          stroke: '#94a3b8',
          lineWidth: (d: any) => Math.min(d.data.weight / 10, 5),
          endArrow: true,
        },
      },
    })

    graph.render()
    graphRef.current = graph

    return () => {
      if (graphRef.current) {
        graphRef.current.destroy()
        graphRef.current = null
      }
    }
  }, [data, height])

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center" style={{ height }}>
        <p className="text-muted-foreground">暂无路径数据</p>
      </div>
    )
  }

  return <div ref={containerRef} className="w-full" />
}
