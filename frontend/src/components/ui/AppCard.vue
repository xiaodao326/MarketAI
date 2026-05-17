<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 内边距档位 */
  padding?: 'none' | 'sm' | 'md' | 'lg'
  /** 是否启用 hover 抬升效果 */
  hoverable?: boolean
  /** 是否带 highlight 顶部色条(品牌色) */
  highlight?: boolean
  /** 自定义 tag,默认 div */
  as?: string
}

const props = withDefaults(defineProps<Props>(), {
  padding: 'md',
  hoverable: false,
  highlight: false,
  as: 'div',
})

const paddingClass = computed(() => {
  switch (props.padding) {
    case 'none': return ''
    case 'sm':   return 'p-4'
    case 'lg':   return 'p-7'
    default:     return 'p-5'
  }
})
</script>

<template>
  <component
    :is="as"
    :class="[
      'relative rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card',
      hoverable && 'transition-all duration-200 ease-out hover:shadow-card-hover hover:-translate-y-px hover:border-brand-200 dark:hover:border-brand-700 cursor-pointer',
      paddingClass,
    ]"
  >
    <div
      v-if="highlight"
      class="absolute top-0 inset-x-0 h-0.5 rounded-t-xl bg-brand-gradient"
    ></div>
    <slot />
  </component>
</template>
