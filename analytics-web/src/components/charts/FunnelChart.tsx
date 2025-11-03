import { useEffect, useRef } from 'react'
import { Chart } from '@antv/g2'
import type { FunnelData } from '@/types/analytics'

interface FunnelChartProps {
  data: FunnelData
  height?: number
}

export function FunnelChart({ data, height = 400 }: FunnelChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<Chart | null>(null)

  useEffect(() => {
    if (!containerRef.current || data.steps.length === 0) return

    // 清除之前的图表
    if (chartRef.current) {
      chartRef.current.destroy()
    }

    // 转换数据格式
    const chartData = data.steps.map((step, index) => ({
      stage: step,
      value: data.counts[index] || 0,
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
      .encode('x', 'stage')
      .encode('y', 'value')
      .encode('color', 'stage')
      .transform({ type: 'symmetryY' })
      .coordinate({ transform: [{ type: 'transpose' }] })
      .axis('y', { title: '用户数' })

    chart.render()
    chartRef.current = chart

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy()
        chartRef.current = null
      }
    }
  }, [data, height])

  if (data.steps.length === 0) {
    return (
      <div className="flex items-center justify-center" style={{ height }}>
        <p className="text-muted-foreground">暂无漏斗数据</p>
      </div>
    )
  }

  return <div ref={containerRef} />
}
