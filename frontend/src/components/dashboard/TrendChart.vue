<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import type { EChartsCoreOption } from 'echarts'
import type { TrendData } from '@/types/dashboard'

const props = defineProps<{
  trendData: TrendData
  timeRange: string
}>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

// 系列名 → 颜色映射（与设计稿一致）
const COLORS: Record<string, string> = {
  '搜索热度': '#378ADD',
  '社媒讨论': '#1D9E75',
  '情感值': '#BA7517',
}

function buildOption(): EChartsCoreOption {
  const showZoom = props.timeRange !== '7d'
  return {
    backgroundColor: 'transparent',
    grid: { top: 30, right: 30, bottom: showZoom ? 62 : 30, left: 50 },
    xAxis: {
      type: 'category',
      data: props.trendData.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
    yAxis: [
      {
        type: 'value',
        axisLabel: { color: '#6b7280', fontSize: 11 },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { color: '#6b7280', fontSize: 11, formatter: '{value}%' },
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
    ],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', crossStyle: { color: '#d1d5db' } },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#374151', fontSize: 12 },
    },
    dataZoom: showZoom
      ? [
          { type: 'inside', start: 0, end: 100 },
          {
            type: 'slider',
            start: 0,
            end: 100,
            bottom: 5,
            height: 18,
            borderColor: '#e5e7eb',
            fillerColor: 'rgba(55,138,221,0.1)',
            handleStyle: { color: '#378ADD' },
            textStyle: { color: '#6b7280', fontSize: 10 },
          },
        ]
      : [],
    series: props.trendData.series.map((s) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      yAxisIndex: s.yaxisIndex,
      data: s.data,
      symbol: 'circle',
      symbolSize: 5,
      showSymbol: false,
      emphasis: { showSymbol: true, scale: 1.5 },
      itemStyle: { color: COLORS[s.name] ?? '#999' },
      lineStyle: {
        color: COLORS[s.name] ?? '#999',
        type: s.name === '情感值' ? 'dashed' : 'solid',
        width: 2,
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: (COLORS[s.name] ?? '#999') + '28' },
          { offset: 1, color: (COLORS[s.name] ?? '#999') + '00' },
        ]),
      },
    })),
  }
}

function resize() {
  chart?.resize()
}

watch(() => props.trendData, () => chart?.setOption(buildOption()), { deep: true })
watch(() => props.timeRange, () => chart?.setOption(buildOption()))

onMounted(() => {
  if (chartEl.value) {
    chart = echarts.init(chartEl.value)
    chart.setOption(buildOption())
  }
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-5">
    <!-- 自定义图例（SVG 折线 + 标签） -->
    <div class="flex items-center gap-6 mb-2">
      <div v-for="s in trendData.series" :key="s.name" class="flex items-center gap-2">
        <svg width="24" height="12" class="flex-shrink-0">
          <line
            x1="0" y1="6" x2="24" y2="6"
            :stroke="COLORS[s.name] ?? '#999'"
            stroke-width="2"
            :stroke-dasharray="s.name === '情感值' ? '4 2' : 'none'"
          />
          <circle cx="12" cy="6" r="2.5" :fill="COLORS[s.name] ?? '#999'" />
        </svg>
        <span class="text-xs text-gray-500 dark:text-gray-400">{{ s.name }}</span>
      </div>
    </div>
    <div ref="chartEl" style="height: 280px; width: 100%;" />
  </div>
</template>
