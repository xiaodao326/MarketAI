<script setup lang="ts">
import { XMarkIcon, ChatBubbleLeftEllipsisIcon } from '@heroicons/vue/24/outline'

// Phase 1 简化:只展示 UI 与目标实体的上下文,实际追问接口在 Phase 2 实现
defineProps<{
  title: string
  context: string
}>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div
        class="absolute inset-0 bg-black/50"
        @click="emit('close')"
      />
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <ChatBubbleLeftEllipsisIcon class="w-5 h-5 text-primary-500" />追问 AI
          </h3>
          <button
            class="p-1 text-gray-400 hover:text-gray-600 transition-colors"
            @click="emit('close')"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <div class="px-6 py-5 space-y-4">
          <div>
            <p class="text-xs text-gray-400 mb-1.5">
              针对
            </p>
            <p class="text-sm font-medium text-gray-900 dark:text-white">
              {{ title }}
            </p>
            <p
              v-if="context"
              class="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-3"
            >
              {{ context }}
            </p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">你的追问</label>
            <textarea
              rows="3"
              disabled
              placeholder="例如:这个痛点在 25-35 岁人群中表现如何?"
              class="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-gray-50 dark:bg-gray-700 text-gray-500 resize-none"
            />
          </div>

          <div class="rounded-lg bg-primary-50 dark:bg-primary-900/20 border border-primary-100 dark:border-primary-800 px-4 py-3">
            <p class="text-sm text-primary-700 dark:text-primary-300">
              该功能即将上线,敬请期待 🎉
            </p>
          </div>
        </div>

        <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-gray-100 dark:border-gray-700">
          <button
            class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors"
            @click="emit('close')"
          >
            关闭
          </button>
          <button
            disabled
            class="px-5 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium opacity-50 cursor-not-allowed"
          >
            提交追问
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
