<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { personaApi } from '@/api/persona'
import {
  UserGroupIcon, SparklesIcon, PlusIcon, ArrowPathIcon,
} from '@heroicons/vue/24/outline'
import PersonaCard from '@/components/personas/PersonaCard.vue'
import GeneratePersonaModal from '@/components/personas/GeneratePersonaModal.vue'
import PersonaCoverageBar from '@/components/personas/PersonaCoverageBar.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppEmpty from '@/components/ui/AppEmpty.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import { toast } from '@/utils/feedback'
import type { Persona, UpdatePersonaPayload } from '@/types/persona'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

const personas = ref<Persona[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const showGenerateModal = ref(false)
const generatingSkeleton = ref(false)

async function loadList() {
  loading.value = true
  error.value = null
  try {
    personas.value = await personaApi.listByProject(projectId.value)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载画像失败'
  } finally {
    loading.value = false
  }
}

async function handleGenerated() {
  showGenerateModal.value = false
  generatingSkeleton.value = true
  toast.success('画像生成完成')
  await loadList()
  generatingSkeleton.value = false
}

async function handleUpdate(id: number, payload: UpdatePersonaPayload) {
  try {
    const updated = await personaApi.update(id, payload)
    const idx = personas.value.findIndex(p => p.id === id)
    if (idx >= 0) personas.value[idx] = updated
    if (payload.isPrimary === true) await loadList()
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : '保存失败')
    await loadList()
  }
}

async function handleDelete(id: number) {
  try {
    await personaApi.remove(id)
    personas.value = personas.value.filter(p => p.id !== id)
    toast.success('画像已删除')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function handleManualAdd() {
  try {
    const created = await personaApi.create({
      projectId: projectId.value,
      name: '新画像',
      role: '请填写职位',
      ageRange: '25-35 岁',
      marketShare: 0,
      isPrimary: false,
      goals: [],
      painPoints: [],
      quote: '',
      decisionParams: {
        paymentWillingness: 'medium',
        decisionCycle: '2-4 周',
        budgetRange: '3-8 万/年',
        techCapability: 'medium',
      },
    })
    personas.value.unshift(created)
    toast.success('画像已创建,请点击右上角菜单进入编辑')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : '创建失败')
  }
}

onMounted(async () => {
  if (!project.value || project.value.id !== projectId.value) {
    try {
      await projectStore.fetchDetail(projectId.value)
    } catch {
      router.replace('/projects')
      return
    }
  }
  await loadList()
})
</script>

<template>
  <div class="max-w-[1400px] mx-auto">
    <PageHeader title="用户画像" backable @back="router.push(`/projects/${projectId}`)">
      <template #subtitle>
        <span v-if="project">{{ project.name }} · 共 {{ personas.length }} 个画像</span>
        <span v-else>AI 生成的目标用户画像与决策参数</span>
      </template>
      <template #actions>
        <button
          :disabled="loading"
          class="w-9 h-9 inline-flex items-center justify-center rounded-md border border-[color:var(--color-border)] text-neutral-500 hover:text-brand-600 hover:border-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors disabled:opacity-40"
          title="刷新"
          @click="loadList"
        >
          <ArrowPathIcon :class="['w-4 h-4', loading && 'animate-spin']" />
        </button>
        <AppButton variant="secondary" @click="handleManualAdd">
          <PlusIcon class="w-4 h-4" />手动添加
        </AppButton>
        <AppButton variant="gradient" @click="showGenerateModal = true">
          <SparklesIcon class="w-4 h-4" />AI 生成
        </AppButton>
      </template>
    </PageHeader>

    <!-- 错误 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-700/40 rounded-lg p-4 flex items-center justify-between mb-5"
    >
      <p class="text-sm text-red-700 dark:text-red-300">{{ error }}</p>
      <AppButton variant="danger" size="sm" @click="loadList">重试</AppButton>
    </div>

    <!-- 空态 -->
    <AppCard v-if="!loading && !generatingSkeleton && !personas.length && !error" padding="lg">
      <AppEmpty
        :icon="UserGroupIcon"
        size="lg"
        title="还没有用户画像"
        description="让 AI 基于项目上下文自动生成 4 个具有差异化的目标画像,或手动添加"
      >
        <template #action>
          <AppButton variant="gradient" size="lg" @click="showGenerateModal = true">
            <SparklesIcon class="w-4 h-4" />让 AI 帮你生成画像
          </AppButton>
        </template>
      </AppEmpty>
    </AppCard>

    <!-- 骨架 -->
    <div
      v-if="(loading && !personas.length) || generatingSkeleton"
      class="grid grid-cols-1 md:grid-cols-2 gap-4"
    >
      <AppCard v-for="i in 4" :key="i" padding="lg">
        <div class="flex items-start gap-3 mb-4">
          <AppSkeleton width="48px" height="48px" rounded="lg" />
          <div class="flex-1 space-y-2">
            <AppSkeleton width="60%" height="16px" />
            <AppSkeleton width="40%" height="12px" />
          </div>
        </div>
        <AppSkeleton :rows="6" height="12px" />
      </AppCard>
    </div>

    <!-- 画像网格 -->
    <div
      v-else-if="personas.length"
      class="grid grid-cols-1 md:grid-cols-2 gap-5 mb-6"
    >
      <PersonaCard
        v-for="p in personas"
        :key="p.id"
        :persona="p"
        editable
        @update="handleUpdate"
        @delete="handleDelete"
      />
    </div>

    <!-- 覆盖度 -->
    <PersonaCoverageBar v-if="personas.length" :personas="personas" />

    <GeneratePersonaModal
      v-if="showGenerateModal"
      :project-id="projectId"
      @close="showGenerateModal = false"
      @completed="handleGenerated"
    />
  </div>
</template>
