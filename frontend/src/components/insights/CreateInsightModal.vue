<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { XMarkIcon, ArrowPathIcon, CheckCircleIcon, ExclamationCircleIcon } from '@heroicons/vue/24/outline'
import { useInsightTask } from '@/composables/useInsightTask'
import type { InsightDepth } from '@/types/insight'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{
  close: []
  completed: [reportId: number]
}>()

const depthOptions: Array<{ value: InsightDepth; label: string; desc: string; eta: string }> = [
  { value: 'lite',     label: '快速', desc: '核心痛点 + 1-2 个机会', eta: '约 30 秒' },
  { value: 'standard', label: '标准', desc: '完整 4 维度分析(推荐)', eta: '约 60 秒' },
  { value: 'full',     label: '完整', desc: '深度分析 + 行动建议', eta: '约 120 秒' },
]

const productDescription = ref('')
const depth = ref<InsightDepth>('standard')

const {
  createTask, reset, taskStatus, isPolling, error, currentReport, displayProgress,
} = useInsightTask()

const charsLeft = computed(() => 200 - productDescription.value.length)
const canSubmit = computed(() =>
  productDescription.value.trim().length > 0 &&
  productDescription.value.length <= 200 &&
  !isPolling.value,
)

// 阶段文案 — 根据进度区间显示当前步骤
const stageLabel = computed(() => {
  if (!taskStatus.value) return ''
  const { status } = taskStatus.value
  if (status === 'pending') return '排队中…'
  if (status === 'completed') return '生成完成'
  if (status === 'failed') return '生成失败'
  const p = displayProgress.value
  if (p < 30) return '正在采集市场数据…'
  if (p < 60) return '正在分析用户痛点…'
  if (p < 85) return '正在生成机会与建议…'
  return '正在整理报告…'
})

async function submit() {
  if (!canSubmit.value) return
  try {
    await createTask({
      projectId: props.projectId,
      productDescription: productDescription.value.trim(),
      depth: depth.value,
    })
  } catch {
    // 错误已在 composable 中写入 error
  }
}

function retry() {
  reset()
}

function close() {
  reset()
  emit('close')
}

// 报告生成完成时,延迟关闭让用户看到 100% 状态
watch(currentReport, (r) => {
  if (r) {
    setTimeout(() => emit('completed', r.id), 600)
  }
})
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div
        class="absolute inset-0 bg-black/50"
        @click="!isPolling && close()"
      />
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            新建 AI 洞察分析
          </h3>
          <button
            :disabled="isPolling"
            class="p-1 text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-40"
            @click="close"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <!-- 表单 (任务未启动时显示) -->
        <div
          v-if="!taskStatus && !error"
          class="px-6 py-5 space-y-5"
        >
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              产品描述 <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="productDescription"
              rows="4"
              maxlength="200"
              placeholder="一句话描述你要分析的产品/市场,例如:面向独立开发者的低代码后端服务"
              class="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 resize-none"
            />
            <p class="text-xs text-gray-400 mt-1 text-right">
              {{ charsLeft }} / 200
            </p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">分析深度</label>
            <div class="space-y-2">
              <label
                v-for="opt in depthOptions"
                :key="opt.value"
                :class="[
                  'flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-colors',
                  depth === opt.value
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                    : 'border-gray-200 dark:border-gray-600 hover:border-gray-300',
                ]"
              >
                <input
                  v-model="depth"
                  type="radio"
                  :value="opt.value"
                  class="mt-1 accent-primary-500"
                >
                <div class="flex-1">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-gray-900 dark:text-white">{{ opt.label }}</span>
                    <span class="text-xs text-gray-400">{{ opt.eta }}</span>
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ opt.desc }}</p>
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- 进度状态 -->
        <div
          v-else-if="taskStatus && !error"
          class="px-6 py-8 space-y-4"
        >
          <div class="flex flex-col items-center">
            <CheckCircleIcon
              v-if="taskStatus.status === 'completed'"
              class="w-12 h-12 text-emerald-500 mb-3"
            />
            <ArrowPathIcon
              v-else
              class="w-12 h-12 text-primary-500 mb-3 animate-spin"
            />
            <p class="text-sm font-medium text-gray-900 dark:text-white">
              {{ stageLabel }}
            </p>
            <p class="text-xs text-gray-400 mt-1">
              任务 ID: #{{ taskStatus.taskId }}
            </p>
          </div>
          <div>
            <div class="flex justify-between text-xs mb-1.5">
              <span class="text-gray-500 dark:text-gray-400">进度</span>
              <span class="text-gray-900 dark:text-white font-medium">{{ displayProgress }}%</span>
            </div>
            <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
              <div
                class="h-full bg-primary-500 rounded-full transition-all duration-500"
                :style="{ width: `${displayProgress}%` }"
              />
            </div>
          </div>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="px-6 py-8 space-y-4"
        >
          <div class="flex flex-col items-center">
            <ExclamationCircleIcon class="w-12 h-12 text-red-500 mb-3" />
            <p class="text-sm font-medium text-gray-900 dark:text-white">
              生成失败
            </p>
            <p class="text-xs text-red-500 dark:text-red-400 mt-2 text-center max-w-xs">
              {{ error }}
            </p>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-gray-100 dark:border-gray-700">
          <button
            v-if="!taskStatus && !error"
            class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors"
            @click="close"
          >
            取消
          </button>
          <button
            v-if="!taskStatus && !error"
            :disabled="!canSubmit"
            class="px-5 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="submit"
          >
            开始分析
          </button>
          <button
            v-else-if="error"
            class="px-5 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors"
            @click="retry"
          >
            重新生成
          </button>
          <button
            v-else-if="isPolling"
            disabled
            class="px-5 py-2 bg-gray-200 dark:bg-gray-600 text-gray-500 rounded-lg text-sm font-medium cursor-not-allowed"
          >
            生成中…
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
