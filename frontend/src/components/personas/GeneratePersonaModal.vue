<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { XMarkIcon, ArrowPathIcon, CheckCircleIcon, ExclamationCircleIcon, UserGroupIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import { usePersonaTask } from '@/composables/usePersonaTask'
import AppButton from '@/components/ui/AppButton.vue'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{
  close: []
  completed: []
}>()

const count = ref<3 | 4 | 5>(4)
const context = ref('')

const {
  createTask, reset, taskStatus, isPolling, isCompleted, error, displayProgress,
} = usePersonaTask()

const charsLeft = computed(() => 500 - context.value.length)
const canSubmit = computed(() => !isPolling.value && context.value.length <= 500)

const stageLabel = computed(() => {
  if (!taskStatus.value) return ''
  const { status } = taskStatus.value
  if (status === 'pending') return '排队中…'
  if (status === 'completed') return '生成完成'
  if (status === 'failed') return '生成失败'
  const p = displayProgress.value
  if (p < 25) return '正在加载项目上下文…'
  if (p < 70) return '正在生成画像细节…'
  if (p < 95) return '正在写入画像数据…'
  return '正在收尾…'
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
      count: count.value,
      context: context.value.trim() || undefined,
    })
  } catch {
    // 错误已在 composable 中
  }
}

function retry() { reset() }

watch(isCompleted, (done) => {
  if (done) setTimeout(() => emit('completed'), 600)
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
          <div class="relative w-full max-w-md rounded-2xl bg-[color:var(--color-surface-elevated)] shadow-overlay border border-[color:var(--color-border)] overflow-hidden">
            <!-- Header -->
            <div class="flex items-start justify-between px-6 py-5 border-b border-[color:var(--color-border)]">
              <div class="flex items-start gap-3">
                <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-purple-500 to-fuchsia-600 flex items-center justify-center flex-shrink-0">
                  <UserGroupIcon class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">AI 生成用户画像</h3>
                  <p class="text-xs text-neutral-500 mt-0.5">基于项目上下文自动生成多个目标画像</p>
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
            <div v-if="!taskStatus && !error" class="px-6 py-5 space-y-5">
              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-2">生成数量</label>
                <div class="grid grid-cols-3 gap-2">
                  <button
                    v-for="n in [3, 4, 5] as const"
                    :key="n"
                    type="button"
                    :class="[
                      'py-2.5 text-sm rounded-md border transition-all',
                      count === n
                        ? 'border-brand-400 bg-brand-50 dark:bg-brand-500/10 text-brand-700 dark:text-brand-300 font-semibold ring-2 ring-brand-500/15'
                        : 'border-[color:var(--color-border)] text-neutral-600 dark:text-neutral-300 hover:border-neutral-300 dark:hover:border-neutral-600',
                    ]"
                    @click="count = n"
                  >{{ n }} 个</button>
                </div>
                <p class="text-xs text-neutral-400 mt-1.5">推荐 4 个 — 兼顾覆盖度与差异化</p>
              </div>

              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                  额外上下文 <span class="text-xs text-neutral-400 font-normal">(选填)</span>
                </label>
                <textarea
                  v-model="context"
                  rows="3"
                  maxlength="500"
                  placeholder="例如:聚焦 25-40 岁一线城市的独立开发者群体"
                  class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15 resize-none"
                />
                <p class="text-xs text-neutral-400 mt-1.5 text-right">{{ charsLeft }} / 500</p>
              </div>

              <p class="text-xs text-neutral-500 dark:text-neutral-400 bg-[color:var(--color-surface-muted)] rounded-lg p-3 leading-relaxed">
                生成的画像会自动结合项目的产品描述、行业、关键词及最近的需求洞察痛点。
              </p>
            </div>

            <!-- 进度 -->
            <div v-else-if="taskStatus && !error" class="px-6 py-10 space-y-5">
              <div class="flex flex-col items-center">
                <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-purple-500 to-fuchsia-600 flex items-center justify-center mb-4 shadow-glow">
                  <CheckCircleIcon v-if="isCompleted" class="w-7 h-7 text-white" />
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
                    class="h-full bg-gradient-to-r from-purple-500 to-fuchsia-500 rounded-full transition-all duration-500"
                    :style="{ width: `${displayProgress}%` }"
                  />
                </div>
              </div>
            </div>

            <!-- 错误 -->
            <div v-else-if="error" class="px-6 py-10 text-center">
              <div class="w-14 h-14 rounded-2xl bg-red-50 dark:bg-red-500/15 flex items-center justify-center mx-auto mb-4">
                <ExclamationCircleIcon class="w-7 h-7 text-red-500" />
              </div>
              <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">生成失败</p>
              <p class="text-xs text-red-500 dark:text-red-400 mt-2 max-w-xs mx-auto">{{ error }}</p>
            </div>

            <!-- Footer -->
            <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <template v-if="!taskStatus && !error">
                <AppButton variant="ghost" @click="close">取消</AppButton>
                <AppButton variant="gradient" :disabled="!canSubmit" @click="submit">
                  <SparklesIcon class="w-4 h-4" />开始生成
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
