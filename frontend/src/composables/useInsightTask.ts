import { ref, computed, onUnmounted } from 'vue'
import { insightApi } from '@/api/insight'
import type { CreateInsightRequest, InsightReport, TaskStatusInfo } from '@/types/insight'

// 轮询配置 — 2s 一次,最长 120s 后超时(覆盖 full 深度 ~120s 估时,留一点余量给最后一轮)
const POLL_INTERVAL_MS = 2000
const TIMEOUT_MS = 120_000

/**
 * 封装"创建任务 → 轮询状态 → 拉取完整报告"全流程
 * 暴露状态供 UI 直接绑定;调用方只需触发 createTask 即可
 */
export function useInsightTask() {
  const taskStatus = ref<TaskStatusInfo | null>(null)
  const currentReport = ref<InsightReport | null>(null)
  const isPolling = ref(false)
  const error = ref<string | null>(null)

  let timer: ReturnType<typeof setInterval> | null = null
  let startTime = 0

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isPolling.value = false
  }

  function reset() {
    stopPolling()
    taskStatus.value = null
    currentReport.value = null
    error.value = null
  }

  async function pollOnce(taskId: number) {
    try {
      const status = await insightApi.getTaskStatus(taskId)
      taskStatus.value = status

      if (status.status === 'completed' && status.resultId) {
        stopPolling()
        currentReport.value = await insightApi.getReport(status.resultId)
        return
      }
      if (status.status === 'failed') {
        stopPolling()
        error.value = status.errorMessage || '任务执行失败'
        return
      }
      // 超时保护
      if (Date.now() - startTime > TIMEOUT_MS) {
        stopPolling()
        error.value = '生成超时,请稍后重试'
      }
    } catch (e: unknown) {
      stopPolling()
      error.value = e instanceof Error ? e.message : '状态查询失败'
    }
  }

  async function createTask(data: CreateInsightRequest) {
    reset()
    try {
      const resp = await insightApi.createInsight(data)
      taskStatus.value = {
        taskId: resp.taskId,
        status: 'pending',
        progress: 0,
      }
      startTime = Date.now()
      isPolling.value = true
      // 立即拉一次,避免首次 2s 延迟
      void pollOnce(resp.taskId)
      timer = setInterval(() => void pollOnce(resp.taskId), POLL_INTERVAL_MS)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '任务创建失败'
      throw e
    }
  }

  // 进度条显示值: pending=5, running=后端 progress, completed=100, failed=保留最后值
  const displayProgress = computed(() => {
    if (!taskStatus.value) return 0
    const { status, progress } = taskStatus.value
    if (status === 'completed') return 100
    if (status === 'pending') return 5
    return Math.max(5, Math.min(progress ?? 0, 99))
  })

  onUnmounted(stopPolling)

  return {
    createTask,
    reset,
    stopPolling,
    taskStatus,
    currentReport,
    isPolling,
    error,
    displayProgress,
  }
}
