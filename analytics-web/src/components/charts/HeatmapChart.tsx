import { useEffect, useRef } from 'react'
import { Chart } from '@antv/g2'
import type { HeatmapData } from '@/types/analytics'

interface HeatmapChartProps {
  data: HeatmapData
  height?: number
}

export function HeatmapChart({ data, height = 400 }: HeatmapChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<Chart | null>(null)

  useEffect(() => {
    if (!containerRef.current || Object.keys(data).length === 0) return

    // 清除之前的图表
    if (chartRef.current) {
      chartRef.current.destroy()
    }

    // 转换数据格式
    const chartData = Object.entries(data).map(([hour, count]) => ({
      hour: `${hour}:00`,
      value: count,
    }))

    // 创建新图表
    const chart = new Chart({
      container: containerRef.current,
      autoFit: true,
      height,
    })

    chart
      .interval()
      .data(chartData)
      .encode('x', 'hour')
      .encode('y', 'value')
      .encode('color', 'value')
      .scale('color', { palette: 'blues' })
      .axis('y', { title: '事件数' })
      .axis('x', { title: '小时' })

    chart.render()
    chartRef.current = chart

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy()
        chartRef.current = null
      }
    }
  }, [data, height])

  if (Object.keys(data).length === 0) {
    return (
      <div className="flex items-center justify-center" style={{ height }}>
        <p className="text-muted-foreground">暂无热点数据</p>
      </div>
    )
  }

  return <div ref={containerRef} />
}
