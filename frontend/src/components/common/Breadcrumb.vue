<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ChevronRightIcon, HomeIcon } from '@heroicons/vue/24/outline'

interface Crumb {
  label: string
  to?: string
}

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

/**
 * 面包屑生成规则:
 * - 从 route.meta.breadcrumb 读取静态段
 * - ':project' 占位符替换为当前项目名,链接到项目详情
 * - 末段无 to(当前页)
 */
const crumbs = computed<Crumb[]>(() => {
  const meta = (route.meta?.breadcrumb as string[]) || []
  if (!meta.length) return []

  const projectId = route.params.id as string | undefined
  const currentProjectName = projectStore.currentProject?.name

  return meta.map((segment, idx) => {
    const isLast = idx === meta.length - 1

    if (segment === ':project') {
      return {
        label: currentProjectName || '当前项目',
        to: isLast ? undefined : projectId ? `/projects/${projectId}` : undefined,
      }
    }

    // 第一段"项目"链回项目列表
    if (segment === '项目' && idx === 0 && !isLast) {
      return { label: segment, to: '/projects' }
    }

    return { label: segment, to: undefined }
  })
})

function go(to?: string) {
  if (to) router.push(to)
}
</script>

<template>
  <nav class="flex items-center gap-1.5 text-sm" aria-label="Breadcrumb">
    <button
      class="flex items-center text-neutral-400 hover:text-brand-500 transition-colors"
      @click="router.push('/dashboard')"
      aria-label="返回工作台"
    >
      <HomeIcon class="w-4 h-4" />
    </button>
    <template v-for="(crumb, idx) in crumbs" :key="idx">
      <ChevronRightIcon class="w-3.5 h-3.5 text-neutral-300 dark:text-neutral-600 flex-shrink-0" />
      <button
        v-if="crumb.to"
        class="text-neutral-500 dark:text-neutral-400 hover:text-brand-500 dark:hover:text-brand-400 transition-colors truncate max-w-[200px]"
        @click="go(crumb.to)"
      >
        {{ crumb.label }}
      </button>
      <span
        v-else
        class="text-neutral-900 dark:text-neutral-100 font-medium truncate max-w-[200px]"
      >
        {{ crumb.label }}
      </span>
    </template>
  </nav>
</template>
