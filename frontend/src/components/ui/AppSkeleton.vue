<script setup lang="ts">
interface Props {
  /** 行数(text 类型)*/
  rows?: number
  /** 宽度 */
  width?: string
  /** 高度 */
  height?: string
  /** 圆角变体 */
  rounded?: 'sm' | 'md' | 'lg' | 'full'
}

withDefaults(defineProps<Props>(), {
  rows: 1,
  width: '100%',
  height: '14px',
  rounded: 'md',
})
</script>

<template>
  <div v-if="rows > 1" class="space-y-2.5">
    <div
      v-for="i in rows"
      :key="i"
      class="ds-skeleton"
      :class="[
        rounded === 'sm' ? 'rounded-sm' :
        rounded === 'lg' ? 'rounded-lg' :
        rounded === 'full' ? 'rounded-full' : 'rounded-md',
      ]"
      :style="{
        width: i === rows ? '60%' : width,
        height,
      }"
    ></div>
  </div>
  <div
    v-else
    class="ds-skeleton"
    :class="[
      rounded === 'sm' ? 'rounded-sm' :
      rounded === 'lg' ? 'rounded-lg' :
      rounded === 'full' ? 'rounded-full' : 'rounded-md',
    ]"
    :style="{ width, height }"
  ></div>
</template>
