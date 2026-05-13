<script setup lang="ts">
import { CheckCircleIcon } from '@heroicons/vue/24/outline'
import type { ActionItem } from '@/types/insight'

defineProps<{ actions: ActionItem[] }>()
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center gap-2 mb-4">
      <CheckCircleIcon class="w-5 h-5 text-primary-500" />
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        行动建议
      </h3>
      <span class="text-xs text-gray-400">{{ actions?.length || 0 }} 项</span>
    </div>

    <div
      v-if="!actions?.length"
      class="text-sm text-gray-400 py-6 text-center"
    >
      暂无建议
    </div>

    <ol class="space-y-3 max-h-[360px] overflow-y-auto pr-1 list-decimal list-inside">
      <li
        v-for="(a, i) in actions"
        :key="i"
        class="border border-gray-100 dark:border-gray-700 rounded-lg p-3"
      >
        <span class="text-sm font-medium text-gray-900 dark:text-white">{{ a.title }}</span>
        <p class="mt-1 ml-5 text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
          {{ a.description }}
        </p>
      </li>
    </ol>
  </div>
</template>
