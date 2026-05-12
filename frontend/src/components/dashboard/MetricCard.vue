<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  icon: Component
  label: string
  value: string | number
  delta: number
  trend: 'up' | 'down' | 'stable'
}>()
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-5">
    <div class="flex items-center gap-3 mb-3">
      <div class="p-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <component :is="icon" class="w-5 h-5 text-primary-600 dark:text-primary-400" />
      </div>
      <span class="text-sm text-gray-500 dark:text-gray-400">{{ label }}</span>
    </div>
    <div class="flex items-baseline justify-between">
      <span class="text-2xl font-bold text-gray-900 dark:text-white">{{ value }}</span>
      <span
        :class="[
          'flex items-center gap-0.5 text-sm font-medium',
          trend === 'up' ? 'text-green-500' : trend === 'down' ? 'text-red-500' : 'text-gray-400',
        ]"
      >
        <span>{{ trend === 'up' ? '↑' : trend === 'down' ? '↓' : '—' }}</span>
        <span>{{ Math.abs(delta).toFixed(1) }}%</span>
      </span>
    </div>
  </div>
</template>
