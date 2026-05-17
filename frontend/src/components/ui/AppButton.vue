<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'gradient'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  block?: boolean
  type?: 'button' | 'submit' | 'reset'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  block: false,
  type: 'button',
})

const sizeCls = computed(() => {
  switch (props.size) {
    case 'sm': return 'h-8 px-3 text-xs'
    case 'lg': return 'h-11 px-6 text-[15px]'
    default:   return 'h-9 px-4 text-sm'
  }
})

const variantCls = computed(() => {
  switch (props.variant) {
    case 'primary':
      return 'bg-brand-500 text-white hover:bg-brand-600 active:bg-brand-700 shadow-sm hover:shadow-glow'
    case 'gradient':
      return 'bg-brand-gradient text-white hover:opacity-90 shadow-glow'
    case 'secondary':
      return 'bg-[color:var(--color-surface)] border border-[color:var(--color-border)] text-neutral-700 dark:text-neutral-200 hover:bg-[color:var(--color-surface-muted)] hover:border-[color:var(--color-border-strong)]'
    case 'ghost':
      return 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800'
    case 'danger':
      return 'bg-red-500 text-white hover:bg-red-600 active:bg-red-700 shadow-sm'
  }
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center gap-1.5 rounded-md font-medium select-none',
      'transition-all duration-150 ease-out',
      'disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none',
      block && 'w-full',
      sizeCls,
      variantCls,
    ]"
  >
    <svg
      v-if="loading"
      class="w-3.5 h-3.5 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" class="opacity-25"></circle>
      <path
        d="M4 12a8 8 0 018-8"
        stroke="currentColor"
        stroke-width="3"
        stroke-linecap="round"
      ></path>
    </svg>
    <slot />
  </button>
</template>
