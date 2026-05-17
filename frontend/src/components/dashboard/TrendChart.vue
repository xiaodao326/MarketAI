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

// 新设计系统色 — 与 brand/accent/warning 对齐
const COLORS: Record<string, string> = {
  '搜索热度': '#6366F1', // brand-500
  '社媒讨论': '#06B6D4', // cyan-500
  '情感值':   '#F59E0B', // amber-500
}

function isDark() {
  return document.documentElement.classList.contains('dark')
}

function buildOption(): EChartsCoreOption {
  const showZoom = props.timeRange !== '7d'
  const dark = isDark()
  const axisColor = dark ? '#A1A1AA' : '#71717A'
  const splitColor = dark ? '#27272A' : '#F4F4F5'
  const borderColor = dark ? '#27272A' : '#E4E4E7'
  const bgColor = dark ? 'rgba(26, 26, 30, 0.95)' : 'rgba(255,255,255,0.96)'
  const textColor = dark ? '#FAFAFA' : '#27272A'

  return {
    backgroundColor: 'transparent',
    grid: { top: 30, right: 30, bottom: showZoom ? 62 : 30, left: 50 },
    xAxis: {
      type: 'category',
      data: props.trendData.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: borderColor } },
      axisTick: { show: false },
      axisLabel: { color: axisColor, fontSize: 11 },
    },
    yAxis: [
      {
        type: 'value',
        axisLabel: { color: axisColor, fontSize: 11 },
        splitLine: { lineStyle: { color: splitColor, type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { color: axisColor, fontSize: 11, formatter: '{value}%' },
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
    ],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', crossStyle: { color: borderColor } },
      backgroundColor: bgColor,
      borderColor: borderColor,
      textStyle: { color: textColor, fontSize: 12 },
      padding: [8, 12],
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.08);',
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
            borderColor: borderColor,
            backgroundColor: 'transparent',
            fillerColor: 'rgba(99,102,241,0.10)',
            handleStyle: { color: '#6366F1' },
            moveHandleStyle: { color: '#6366F1' },
            textStyle: { color: axisColor, fontSize: 10 },
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
        width: 2.5,
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

function resize() { chart?.resize() }
function rerender() { chart?.setOption(buildOption()) }

watch(() => props.trendData, rerender, { deep: true })
watch(() => props.timeRange, rerender)

let themeObserver: MutationObserver | null = null

onMounted(() => {
  if (chartEl.value) {
    chart = echarts.init(chartEl.value)
    chart.setOption(buildOption())
  }
  window.addEventListener('resize', resize)
  // 监听 dark class 变化以同步主题
  themeObserver = new MutationObserver(rerender)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  themeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-5">
    <!-- 图例 -->
    <div class="flex items-center justify-between mb-3 flex-wrap gap-3">
      <h3 class="text-sm font-semibold text-neutral-700 dark:text-neutral-200">趋势走势</h3>
      <div class="flex items-center gap-4">
        <div v-for="s in trendData.series" :key="s.name" class="flex items-center gap-1.5">
          <svg width="20" height="10" class="flex-shrink-0">
            <line
              x1="0" y1="5" x2="20" y2="5"
              :stroke="COLORS[s.name] ?? '#999'"
              stroke-width="2"
              :stroke-dasharray="s.name === '情感值' ? '3 2' : 'none'"
            />
            <circle cx="10" cy="5" r="2.5" :fill="COLORS[s.name] ?? '#999'" />
          </svg>
          <span class="text-xs text-neutral-600 dark:text-neutral-300">{{ s.name }}</span>
        </div>
      </div>
    </div>
    <div ref="chartEl" style="height: 300px; width: 100%;" />
  </div>
</template>
