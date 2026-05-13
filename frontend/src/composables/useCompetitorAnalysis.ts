import { ref, computed, onUnmounted } from 'vue'
import { competitorApi } from '@/api/competitor'
import type { AnalyzeCompetitorsRequest, CompetitorTaskInfo } from '@/types/competitor'

const POLL_INTERVAL_MS = 2000
const TIMEOUT_MS = 150_000 // 竞品分析估时 ~60s, 余量到 150s

/**
 * 竞品分析任务: 创建 → 轮询 → 完成
 * 完成后 isCompleted=true, 由调用方拉新数据 (后端没有单一 resultId)
 */
export function useCompetitorAnalysis() {
  const taskStatus = ref<CompetitorTaskInfo | null>(null)
  const isPolling = ref(false)
  const isCompleted = ref(false)
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
    isCompleted.value = false
    error.value = null
  }

  async function pollOnce(taskId: number) {
    try {
      const status = await competitorApi.getTaskStatus(taskId)
      taskStatus.value = status

      if (status.status === 'completed') {
        stopPolling()
        isCompleted.value = true
        return
      }
      if (status.status === 'failed') {
        stopPolling()
        error.value = status.errorMessage || '竞品分析失败'
        return
      }
      if (Date.now() - startTime > TIMEOUT_MS) {
        stopPolling()
        error.value = '分析超时,请稍后重试'
      }
    } catch (e: unknown) {
      stopPolling()
      error.value = e instanceof Error ? e.message : '状态查询失败'
    }
  }

  async function createTask(data: AnalyzeCompetitorsRequest) {
    reset()
    try {
      const resp = await competitorApi.analyze(data)
      taskStatus.value = {
        taskId: resp.taskId,
        status: 'pending',
        progress: 0,
        projectId: data.projectId,
      }
      startTime = Date.now()
      isPolling.value = true
      void pollOnce(resp.taskId)
      timer = setInterval(() => void pollOnce(resp.taskId), POLL_INTERVAL_MS)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '任务创建失败'
      throw e
    }
  }

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
    isPolling,
    isCompleted,
    error,
    displayProgress,
  }
}
