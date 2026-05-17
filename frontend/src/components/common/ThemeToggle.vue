<script setup lang="ts">
import { onMounted } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { SunIcon, MoonIcon, ComputerDesktopIcon } from '@heroicons/vue/24/outline'

const { mode, toggle, init } = useTheme()

onMounted(() => init())
</script>

<template>
  <button
    type="button"
    class="flex items-center justify-center w-9 h-9 rounded-md text-neutral-500 dark:text-neutral-400 hover:text-brand-600 dark:hover:text-brand-300 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-all"
    :title="mode === 'light' ? '当前:浅色' : mode === 'dark' ? '当前:暗色' : '当前:跟随系统'"
    @click="toggle"
  >
    <SunIcon v-if="mode === 'light'" class="w-5 h-5" />
    <MoonIcon v-else-if="mode === 'dark'" class="w-5 h-5" />
    <ComputerDesktopIcon v-else class="w-5 h-5" />
  </button>
</template>
