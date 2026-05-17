<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import {
  PlusIcon, MagnifyingGlassIcon, FolderOpenIcon, TrashIcon,
  CalendarDaysIcon, TagIcon, BuildingOffice2Icon,
} from '@heroicons/vue/24/outline'
import ProjectCreateModal from './ProjectCreateModal.vue'
import type { Project } from '@/types/project'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppEmpty from '@/components/ui/AppEmpty.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import { confirm, toast } from '@/utils/feedback'

const router = useRouter()
const projectStore = useProjectStore()

const searchKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const showCreateModal = ref(false)
const deletingId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = 12

const statusOptions = [
  { value: undefined, label: '全部' },
  { value: 1, label: '活跃' },
  { value: 0, label: '草稿' },
  { value: 2, label: '归档' },
]

function statusBadge(status: number): { label: string; tone: 'neutral' | 'success' | 'warning' } {
  switch (status) {
    case 0: return { label: '草稿', tone: 'neutral' }
    case 1: return { label: '活跃', tone: 'success' }
    case 2: return { label: '归档', tone: 'warning' }
    default: return { label: '未知', tone: 'neutral' }
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(projectStore.total / pageSize)))

async function loadProjects() {
  await projectStore.fetchList({
    page: currentPage.value,
    size: pageSize,
    status: statusFilter.value,
    keyword: searchKeyword.value || undefined,
  })
}

function onSearch() {
  currentPage.value = 1
  loadProjects()
}

function onStatusChange(status: number | undefined) {
  statusFilter.value = status
  currentPage.value = 1
  loadProjects()
}

function goToDetail(project: Project) {
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

async function handleDelete(project: Project) {
  const ok = await confirm(
    `项目「${project.name}」归档后不再展示在活跃列表,可随时通过筛选查看。`,
    `归档项目`,
    { type: 'warning', confirmText: '归档', danger: true },
  )
  if (!ok) return
  deletingId.value = project.id
  try {
    await projectStore.remove(project.id)
    toast.success('项目已归档')
  } catch (e) {
    toast.error((e as Error).message || '归档失败')
  } finally {
    deletingId.value = null
  }
}

function handleCreated(project: Project) {
  showCreateModal.value = false
  toast.success('项目创建成功')
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

function goToPage(page: number) {
  currentPage.value = page
  loadProjects()
}

onMounted(() => loadProjects().catch(() => {}))
</script>

<template>
  <div class="max-w-[1400px] mx-auto">
    <PageHeader title="分析项目" subtitle="管理你的所有市场分析项目">
      <template #actions>
        <AppButton variant="gradient" @click="showCreateModal = true">
          <PlusIcon class="w-4 h-4" />新建项目
        </AppButton>
      </template>
    </PageHeader>

    <!-- 工具栏:搜索 + 状态筛选 -->
    <AppCard padding="sm" class="mb-5">
      <div class="flex items-center gap-3 flex-wrap">
        <div class="relative flex-1 min-w-[220px] max-w-md">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索项目名称…"
            class="w-full pl-9 pr-3 py-2 rounded-md bg-[color:var(--color-surface-muted)] border border-transparent hover:border-[color:var(--color-border)] focus:bg-[color:var(--color-surface)] focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15 focus:outline-none text-sm transition-all"
            @keyup.enter="onSearch"
          />
        </div>
        <div class="inline-flex items-center p-0.5 rounded-md bg-[color:var(--color-surface-muted)] border border-[color:var(--color-border)]">
          <button
            v-for="opt in statusOptions"
            :key="opt.value ?? '_all'"
            type="button"
            :class="[
              'px-3 py-1.5 text-xs font-medium rounded transition-all',
              statusFilter === opt.value
                ? 'bg-[color:var(--color-surface)] text-neutral-900 dark:text-neutral-100 shadow-sm'
                : 'text-neutral-500 hover:text-neutral-700 dark:hover:text-neutral-200',
            ]"
            @click="onStatusChange(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>
    </AppCard>

    <!-- 加载态 -->
    <div v-if="projectStore.loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <AppCard v-for="i in 6" :key="i">
        <AppSkeleton height="18px" width="70%" />
        <AppSkeleton class="mt-3" height="12px" width="40%" />
        <AppSkeleton class="mt-4" :rows="2" height="10px" />
      </AppCard>
    </div>

    <!-- 空态 -->
    <AppCard v-else-if="!projectStore.hasProjects" padding="lg">
      <AppEmpty
        :icon="FolderOpenIcon"
        size="lg"
        title="还没有项目"
        :description="searchKeyword || statusFilter !== undefined ? '没有匹配的项目,试试调整筛选条件' : '输入目标市场关键词,创建你的第一个分析项目'"
      >
        <template #action>
          <AppButton variant="gradient" size="lg" @click="showCreateModal = true">
            <PlusIcon class="w-4 h-4" />开始新项目
          </AppButton>
        </template>
      </AppEmpty>
    </AppCard>

    <!-- 项目网格 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <AppCard
        v-for="project in projectStore.projectList"
        :key="project.id"
        hoverable
        class="group flex flex-col"
        @click="goToDetail(project)"
      >
        <!-- 顶部:名称 + 状态 -->
        <div class="flex items-start justify-between gap-3 mb-3">
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold text-[15px] text-neutral-900 dark:text-neutral-100 truncate group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors">
              {{ project.name }}
            </h3>
            <p
              v-if="project.description"
              class="text-xs text-neutral-500 dark:text-neutral-400 mt-1 line-clamp-2"
            >
              {{ project.description }}
            </p>
          </div>
          <AppBadge :tone="statusBadge(project.status).tone" dot>{{ statusBadge(project.status).label }}</AppBadge>
        </div>

        <!-- 行业 + 市场 -->
        <div class="flex items-center gap-2 mb-3 flex-wrap">
          <AppBadge tone="brand" outline>
            <BuildingOffice2Icon class="w-3 h-3" />
            {{ project.industry }}
          </AppBadge>
          <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ project.targetMarket }}</span>
        </div>

        <!-- 关键词预览 -->
        <div v-if="project.keywords?.length" class="flex flex-wrap gap-1 mb-4">
          <span
            v-for="kw in project.keywords.slice(0, 4)"
            :key="kw"
            class="text-[11px] px-1.5 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-300"
          >
            {{ kw }}
          </span>
          <span
            v-if="project.keywords.length > 4"
            class="text-[11px] px-1.5 py-0.5 text-neutral-400"
          >+{{ project.keywords.length - 4 }}</span>
        </div>

        <!-- 底部:meta + 操作 -->
        <div class="mt-auto pt-3 border-t border-[color:var(--color-border)] flex items-center justify-between">
          <div class="flex items-center gap-3 text-[11px] text-neutral-500 dark:text-neutral-400">
            <span class="inline-flex items-center gap-1">
              <TagIcon class="w-3 h-3" />{{ project.keywords?.length || 0 }}
            </span>
            <span class="inline-flex items-center gap-1">
              <CalendarDaysIcon class="w-3 h-3" />{{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}
            </span>
          </div>
          <button
            class="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded text-neutral-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10"
            :disabled="deletingId === project.id"
            title="归档项目"
            @click.stop="handleDelete(project)"
          >
            <TrashIcon class="w-4 h-4" />
          </button>
        </div>
      </AppCard>
    </div>

    <!-- 分页 -->
    <div v-if="!projectStore.loading && totalPages > 1" class="flex items-center justify-center gap-1 mt-8">
      <button
        v-for="p in totalPages"
        :key="p"
        type="button"
        :class="[
          'min-w-[36px] h-9 px-3 text-sm rounded-md transition-all',
          p === currentPage
            ? 'bg-brand-500 text-white shadow-sm'
            : 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800',
        ]"
        @click="goToPage(p)"
      >
        {{ p }}
      </button>
    </div>

    <ProjectCreateModal
      v-if="showCreateModal"
      @close="showCreateModal = false"
      @created="handleCreated"
    />
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
