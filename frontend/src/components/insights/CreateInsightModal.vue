<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { XMarkIcon, ArrowPathIcon, CheckCircleIcon, ExclamationCircleIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import { useInsightTask } from '@/composables/useInsightTask'
import type { InsightDepth } from '@/types/insight'
import AppButton from '@/components/ui/AppButton.vue'

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

const visible = ref(true)
function close() {
  if (isPolling.value) return
  visible.value = false
  setTimeout(() => {
    reset()
    emit('close')
  }, 180)
}

async function submit() {
  if (!canSubmit.value) return
  try {
    await createTask({
      projectId: props.projectId,
      productDescription: productDescription.value.trim(),
      depth: depth.value,
    })
  } catch {
    // 错误已在 composable 中
  }
}

function retry() { reset() }

watch(currentReport, (r) => {
  if (r) {
    setTimeout(() => emit('completed', r.id), 600)
  }
})
</script>

<template>
  <Teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-neutral-900/60 backdrop-blur-sm" @click="!isPolling && close()" />

        <transition
          appear
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
        >
          <div class="relative w-full max-w-lg rounded-2xl bg-[color:var(--color-surface-elevated)] shadow-overlay border border-[color:var(--color-border)] overflow-hidden max-h-[92vh] flex flex-col">
            <!-- Header -->
            <div class="flex items-start justify-between px-6 py-5 border-b border-[color:var(--color-border)]">
              <div class="flex items-start gap-3">
                <div class="w-10 h-10 rounded-lg bg-brand-gradient flex items-center justify-center flex-shrink-0">
                  <SparklesIcon class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">新建 AI 洞察分析</h3>
                  <p class="text-xs text-neutral-500 mt-0.5">让 AI 自动生成结构化的市场分析报告</p>
                </div>
              </div>
              <button
                :disabled="isPolling"
                class="p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800 disabled:opacity-40"
                @click="close"
              >
                <XMarkIcon class="w-5 h-5" />
              </button>
            </div>

            <!-- 表单 -->
            <div v-if="!taskStatus && !error" class="flex-1 overflow-y-auto px-6 py-5 space-y-5">
              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                  产品描述 <span class="text-red-500">*</span>
                </label>
                <textarea
                  v-model="productDescription"
                  rows="4"
                  maxlength="200"
                  placeholder="一句话描述你要分析的产品/市场,例如:面向独立开发者的低代码后端服务"
                  class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15 resize-none"
                />
                <p class="text-xs text-neutral-400 mt-1.5 text-right">{{ charsLeft }} / 200</p>
              </div>

              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-2">分析深度</label>
                <div class="space-y-2">
                  <label
                    v-for="opt in depthOptions"
                    :key="opt.value"
                    :class="[
                      'flex items-start gap-3 p-3.5 rounded-lg border cursor-pointer transition-all',
                      depth === opt.value
                        ? 'border-brand-400 bg-brand-50 dark:bg-brand-500/10 ring-2 ring-brand-500/15'
                        : 'border-[color:var(--color-border)] hover:border-neutral-300 dark:hover:border-neutral-600',
                    ]"
                  >
                    <input v-model="depth" type="radio" :value="opt.value" class="mt-1 accent-brand-500" />
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2">
                        <span class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ opt.label }}</span>
                        <span class="text-xs text-neutral-400 flex-shrink-0">{{ opt.eta }}</span>
                      </div>
                      <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">{{ opt.desc }}</p>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            <!-- 进度 -->
            <div v-else-if="taskStatus && !error" class="flex-1 px-6 py-10 space-y-5">
              <div class="flex flex-col items-center">
                <div class="w-14 h-14 rounded-2xl bg-brand-gradient flex items-center justify-center mb-4 shadow-glow">
                  <CheckCircleIcon v-if="taskStatus.status === 'completed'" class="w-7 h-7 text-white" />
                  <ArrowPathIcon v-else class="w-7 h-7 text-white animate-spin" />
                </div>
                <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ stageLabel }}</p>
                <p class="text-xs text-neutral-400 mt-1">任务 ID: #{{ taskStatus.taskId }}</p>
              </div>
              <div class="max-w-sm mx-auto">
                <div class="flex justify-between text-xs mb-1.5">
                  <span class="text-neutral-500 dark:text-neutral-400">进度</span>
                  <span class="text-neutral-900 dark:text-neutral-100 font-semibold tabular-nums">{{ displayProgress }}%</span>
                </div>
                <div class="h-2 bg-neutral-100 dark:bg-neutral-800 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-brand-gradient rounded-full transition-all duration-500"
                    :style="{ width: `${displayProgress}%` }"
                  />
                </div>
              </div>
            </div>

            <!-- 错误 -->
            <div v-else-if="error" class="flex-1 px-6 py-10">
              <div class="flex flex-col items-center text-center">
                <div class="w-14 h-14 rounded-2xl bg-red-50 dark:bg-red-500/15 flex items-center justify-center mb-4">
                  <ExclamationCircleIcon class="w-7 h-7 text-red-500" />
                </div>
                <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">生成失败</p>
                <p class="text-xs text-red-500 dark:text-red-400 mt-2 max-w-xs">{{ error }}</p>
              </div>
            </div>

            <!-- Footer -->
            <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <template v-if="!taskStatus && !error">
                <AppButton variant="ghost" @click="close">取消</AppButton>
                <AppButton variant="gradient" :disabled="!canSubmit" @click="submit">
                  <SparklesIcon class="w-4 h-4" />开始分析
                </AppButton>
              </template>
              <template v-else-if="error">
                <AppButton variant="primary" @click="retry">重新生成</AppButton>
              </template>
              <template v-else-if="isPolling">
                <AppButton variant="secondary" disabled :loading="true">生成中…</AppButton>
              </template>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>
