<script setup lang="ts">
interface Props {
  title: string
  subtitle?: string
  /** 标题字号档位 */
  level?: 'h1' | 'h2' | 'h3'
}

withDefaults(defineProps<Props>(), {
  subtitle: '',
  level: 'h2',
})
</script>

<template>
  <div class="flex items-end justify-between gap-4 flex-wrap">
    <div class="min-w-0">
      <component
        :is="level"
        :class="[
          'font-semibold text-neutral-900 dark:text-neutral-100 tracking-tight',
          level === 'h1' ? 'text-[26px] leading-tight' : level === 'h2' ? 'text-xl' : 'text-base',
        ]"
      >
        {{ title }}
      </component>
      <p
        v-if="subtitle || $slots.subtitle"
        class="text-sm text-neutral-500 dark:text-neutral-400 mt-1.5"
      >
        <slot name="subtitle">{{ subtitle }}</slot>
      </p>
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2 flex-shrink-0">
      <slot name="actions" />
    </div>
  </div>
</template>
