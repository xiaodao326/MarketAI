import { ref, watch, onUnmounted, isRef, type Ref } from 'vue'
import { dashboardApi } from '@/api/dashboard'
import type { Metrics, TrendData, KeywordRank, Anomaly } from '@/types/dashboard'

export function useDashboard(projectIdInput: Ref<number> | number) {
  const projectId = isRef(projectIdInput) ? projectIdInput : ref(projectIdInput)

  const timeRange = ref<'7d' | '30d' | '90d'>('7d')
  const metrics = ref<Metrics | null>(null)
  const trendData = ref<TrendData | null>(null)
  const topKeywords = ref<KeywordRank[]>([])
  const anomalies = ref<Anomaly[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function refresh() {
    if (!projectId.value) return
    loading.value = true
    error.value = null
    try {
      const [m, t, k, a] = await Promise.all([
        dashboardApi.getMetrics(projectId.value, timeRange.value),
        dashboardApi.getTrend(projectId.value, timeRange.value),
        dashboardApi.getTopKeywords(projectId.value, 10),
        dashboardApi.getAnomalies(projectId.value, 10),
      ])
      metrics.value = m
      trendData.value = t
      topKeywords.value = k
      anomalies.value = a
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '数据加载失败'
    } finally {
      loading.value = false
    }
  }

  watch([projectId, timeRange], refresh, { immediate: true })

  // 每 5 分钟自动刷新
  const timer = setInterval(refresh, 5 * 60 * 1000)
  onUnmounted(() => clearInterval(timer))

  return { metrics, trendData, topKeywords, anomalies, timeRange, loading, error, refresh }
}
