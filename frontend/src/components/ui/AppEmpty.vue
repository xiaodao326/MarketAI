<script setup lang="ts">
import { InboxIcon } from '@heroicons/vue/24/outline'
import type { Component } from 'vue'

interface Props {
  icon?: Component
  title?: string
  description?: string
  /** 视觉尺寸 */
  size?: 'sm' | 'md' | 'lg'
}

withDefaults(defineProps<Props>(), {
  icon: InboxIcon,
  title: '暂无数据',
  description: '',
  size: 'md',
})
</script>

<template>
  <div
    class="flex flex-col items-center justify-center text-center"
    :class="size === 'sm' ? 'py-8' : size === 'lg' ? 'py-20' : 'py-14'"
  >
    <div
      class="rounded-2xl bg-gradient-to-br from-neutral-100 to-neutral-50 dark:from-neutral-800 dark:to-neutral-900 flex items-center justify-center mb-4 ring-1 ring-neutral-200 dark:ring-neutral-700"
      :class="size === 'sm' ? 'w-12 h-12' : size === 'lg' ? 'w-20 h-20' : 'w-16 h-16'"
    >
      <component
        :is="icon"
        class="text-neutral-400 dark:text-neutral-500"
        :class="size === 'sm' ? 'w-6 h-6' : size === 'lg' ? 'w-10 h-10' : 'w-8 h-8'"
      />
    </div>
    <h3
      class="font-medium text-neutral-900 dark:text-neutral-100"
      :class="size === 'sm' ? 'text-sm' : 'text-[15px]'"
    >
      {{ title }}
    </h3>
    <p
      v-if="description || $slots.description"
      class="text-sm text-neutral-500 dark:text-neutral-400 mt-1.5 max-w-sm"
    >
      <slot name="description">{{ description }}</slot>
    </p>
    <div v-if="$slots.action" class="mt-5">
      <slot name="action" />
    </div>
  </div>
</template>
