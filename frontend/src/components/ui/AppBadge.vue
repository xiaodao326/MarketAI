<script setup lang="ts">
import { computed } from 'vue'

type Tone = 'brand' | 'success' | 'warning' | 'danger' | 'neutral' | 'info' | 'accent'

interface Props {
  tone?: Tone
  /** 点状指示 */
  dot?: boolean
  /** 边框变体 */
  outline?: boolean
  size?: 'sm' | 'md'
}

const props = withDefaults(defineProps<Props>(), {
  tone: 'neutral',
  dot: false,
  outline: false,
  size: 'sm',
})

const toneCls = computed(() => {
  const filled: Record<Tone, string> = {
    brand:   'bg-brand-50 text-brand-700 dark:bg-brand-500/12 dark:text-brand-300',
    success: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/12 dark:text-emerald-300',
    warning: 'bg-amber-50 text-amber-700 dark:bg-amber-500/12 dark:text-amber-300',
    danger:  'bg-red-50 text-red-700 dark:bg-red-500/12 dark:text-red-300',
    info:    'bg-sky-50 text-sky-700 dark:bg-sky-500/12 dark:text-sky-300',
    accent:  'bg-cyan-50 text-cyan-700 dark:bg-cyan-500/12 dark:text-cyan-300',
    neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-700/40 dark:text-neutral-300',
  }
  const outlined: Record<Tone, string> = {
    brand:   'ring-1 ring-brand-200 dark:ring-brand-700 text-brand-700 dark:text-brand-300',
    success: 'ring-1 ring-emerald-200 dark:ring-emerald-700 text-emerald-700 dark:text-emerald-300',
    warning: 'ring-1 ring-amber-200 dark:ring-amber-700 text-amber-700 dark:text-amber-300',
    danger:  'ring-1 ring-red-200 dark:ring-red-700 text-red-700 dark:text-red-300',
    info:    'ring-1 ring-sky-200 dark:ring-sky-700 text-sky-700 dark:text-sky-300',
    accent:  'ring-1 ring-cyan-200 dark:ring-cyan-700 text-cyan-700 dark:text-cyan-300',
    neutral: 'ring-1 ring-neutral-200 dark:ring-neutral-700 text-neutral-700 dark:text-neutral-300',
  }
  return props.outline ? outlined[props.tone] : filled[props.tone]
})

const dotCls = computed<Record<Tone, string>>(() => ({
  brand: 'bg-brand-500',
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger: 'bg-red-500',
  info: 'bg-sky-500',
  accent: 'bg-cyan-500',
  neutral: 'bg-neutral-400',
}))
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1 rounded-full font-medium whitespace-nowrap',
      size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-[13px]',
      toneCls,
    ]"
  >
    <span
      v-if="dot"
      class="inline-block w-1.5 h-1.5 rounded-full"
      :class="dotCls[tone]"
    ></span>
    <slot />
  </span>
</template>
