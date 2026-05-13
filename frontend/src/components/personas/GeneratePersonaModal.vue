<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { XMarkIcon, ArrowPathIcon, CheckCircleIcon, ExclamationCircleIcon, UserGroupIcon } from '@heroicons/vue/24/outline'
import { usePersonaTask } from '@/composables/usePersonaTask'

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

function close() {
  reset()
  emit('close')
}

// 完成后延迟关闭让用户看到 100%
watch(isCompleted, (done) => {
  if (done) setTimeout(() => emit('completed'), 600)
})
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div
        class="absolute inset-0 bg-black/50"
        @click="!isPolling && close()"
      />
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <UserGroupIcon class="w-5 h-5 text-primary-500" />AI 生成用户画像
          </h3>
          <button
            :disabled="isPolling"
            class="p-1 text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-40"
            @click="close"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <!-- 表单 -->
        <div
          v-if="!taskStatus && !error"
          class="px-6 py-5 space-y-5"
        >
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">生成数量</label>
            <div class="grid grid-cols-3 gap-2">
              <button
                v-for="n in [3, 4, 5] as const"
                :key="n"
                type="button"
                :class="[
                  'py-2 text-sm rounded-lg border transition-colors',
                  count === n
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300 font-medium'
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:border-gray-300',
                ]"
                @click="count = n"
              >
                {{ n }} 个
              </button>
            </div>
            <p class="text-xs text-gray-400 mt-1.5">
              推荐 4 个 — 兼顾覆盖度与差异化
            </p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              额外上下文 <span class="text-xs text-gray-400 font-normal">(选填)</span>
            </label>
            <textarea
              v-model="context"
              rows="3"
              maxlength="500"
              placeholder="例如:聚焦 25-40 岁一线城市的独立开发者群体"
              class="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 resize-none"
            />
            <p class="text-xs text-gray-400 mt-1 text-right">
              {{ charsLeft }} / 500
            </p>
          </div>

          <p class="text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-900/30 rounded-lg p-3">
            生成的画像会自动结合项目的产品描述、行业、关键词及最近的需求洞察痛点。
          </p>
        </div>

        <!-- 进度 -->
        <div
          v-else-if="taskStatus && !error"
          class="px-6 py-8 space-y-4"
        >
          <div class="flex flex-col items-center">
            <CheckCircleIcon
              v-if="isCompleted"
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

        <!-- 错误 -->
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
            开始生成
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
