import { useEffect, useRef } from 'react'
import { Chart } from '@antv/g2'

interface PieChartProps {
  data: Array<{ label: string; value: number }>
  height?: number
}

export function PieChart({ data, height = 400 }: PieChartProps) {
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
      .interval()
      .data(data)
      .transform({ type: 'stackY' })
      .coordinate({ type: 'theta', outerRadius: 0.8 })
      .encode('y', 'value')
      .encode('color', 'label')
      .legend('color', { position: 'right' })
      .label({
        text: 'label',
        style: {
          fontWeight: 'bold',
        },
      })

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
        <p className="text-muted-foreground">暂无分群数据</p>
      </div>
    )
  }

  return <div ref={containerRef} />
}
