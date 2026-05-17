<script setup lang="ts">
interface Props {
  title: string
  subtitle?: string
  /** 是否带左侧返回按钮 */
  backable?: boolean
}

withDefaults(defineProps<Props>(), {
  subtitle: '',
  backable: false,
})

const emit = defineEmits<{ (e: 'back'): void }>()
</script>

<template>
  <div class="flex items-start justify-between gap-4 flex-wrap mb-6">
    <div class="flex items-start gap-3 min-w-0">
      <button
        v-if="backable"
        type="button"
        class="mt-1 w-9 h-9 flex items-center justify-center rounded-md border border-[color:var(--color-border)] text-neutral-500 hover:text-brand-600 hover:border-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors flex-shrink-0"
        @click="emit('back')"
        aria-label="返回"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="min-w-0">
        <h1 class="text-[22px] font-semibold text-neutral-900 dark:text-neutral-100 tracking-tight leading-tight">
          {{ title }}
        </h1>
        <p
          v-if="subtitle || $slots.subtitle"
          class="text-sm text-neutral-500 dark:text-neutral-400 mt-1.5"
        >
          <slot name="subtitle">{{ subtitle }}</slot>
        </p>
      </div>
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2 flex-shrink-0">
      <slot name="actions" />
    </div>
  </div>
</template>
