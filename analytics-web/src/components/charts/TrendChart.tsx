import { useEffect, useRef } from 'react'
import { Chart } from '@antv/g2'
import type { TrendDataPoint } from '@/types/analytics'

interface TrendChartProps {
  data: TrendDataPoint[]
  height?: number
}

export function TrendChart({ data, height = 400 }: TrendChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<Chart | null>(null)

  useEffect(() => {
    if (!containerRef.current || data.length === 0) return

    // 清除之前的图表
    if (chartRef.current) {
      chartRef.current.destroy()
    }

    // 创建新图表
    const chart = new Chart({
      container: containerRef.current,
      autoFit: true,
      height,
    })

    chart
      .line()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'count')
      .encode('color', () => '事件数')
      .style('stroke', '#3b82f6')
      .style('lineWidth', 3)
      .axis('y', { title: '事件数' })
      .axis('x', { title: '日期' })

    chart
      .point()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'count')
      .encode('color', () => '事件数')
      .encode('shape', 'point')
      .encode('size', 5)
      .style('fill', '#3b82f6')

    chart.render()
    chartRef.current = chart

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy()
        chartRef.current = null
      }
    }
  }, [data, height])

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center" style={{ height }}>
        <p className="text-muted-foreground">暂无趋势数据</p>
      </div>
    )
  }

  return <div ref={containerRef} />
}
