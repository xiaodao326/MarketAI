<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { personaApi } from '@/api/persona'
import {
  ArrowLeftIcon, UserGroupIcon, SparklesIcon, PlusIcon, ArrowPathIcon,
} from '@heroicons/vue/24/outline'
import PersonaCard from '@/components/personas/PersonaCard.vue'
import GeneratePersonaModal from '@/components/personas/GeneratePersonaModal.vue'
import PersonaCoverageBar from '@/components/personas/PersonaCoverageBar.vue'
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
const generatingSkeleton = ref(false) // 生成中显示骨架屏

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
  await loadList()
  generatingSkeleton.value = false
}

async function handleUpdate(id: number, payload: UpdatePersonaPayload) {
  try {
    const updated = await personaApi.update(id, payload)
    // 局部替换,避免全量重拉造成滚动跳转
    const idx = personas.value.findIndex(p => p.id === id)
    if (idx >= 0) personas.value[idx] = updated
    // 若改了 isPrimary=true,其他卡片的 isPrimary 由后端置 0,需要刷新
    if (payload.isPrimary === true) await loadList()
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '保存失败')
    // 失败后回滚 — 重拉一遍
    await loadList()
  }
}

async function handleDelete(id: number) {
  try {
    await personaApi.remove(id)
    personas.value = personas.value.filter(p => p.id !== id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

// 手动添加占位画像 — Phase 1 简化:用默认值创建,用户在卡片内编辑
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
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '创建失败')
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
  <div>
    <!-- 顶部工具栏 -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
      <div class="flex items-center gap-3">
        <button
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          title="返回项目"
          @click="router.push(`/projects/${projectId}`)"
        >
          <ArrowLeftIcon class="w-5 h-5" />
        </button>
        <div>
          <h1 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <UserGroupIcon class="w-5 h-5 text-purple-500" />用户画像
          </h1>
          <p
            v-if="project"
            class="text-sm text-gray-500 dark:text-gray-400"
          >
            {{ project.name }} · 共 {{ personas.length }} 个画像
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          :disabled="loading"
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-40"
          title="刷新"
          @click="loadList"
        >
          <ArrowPathIcon :class="['w-5 h-5', loading && 'animate-spin']" />
        </button>
        <button
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          @click="handleManualAdd"
        >
          <PlusIcon class="w-4 h-4" />手动添加
        </button>
        <button
          class="inline-flex items-center gap-1.5 px-4 py-1.5 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          @click="showGenerateModal = true"
        >
          <SparklesIcon class="w-4 h-4" />AI 生成画像
        </button>
      </div>
    </div>

    <!-- 错误 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-5 flex items-center justify-between mb-6"
    >
      <p class="text-red-600 dark:text-red-400 text-sm">
        {{ error }}
      </p>
      <button
        class="ml-4 px-4 py-1.5 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg transition-colors flex-shrink-0"
        @click="loadList"
      >
        重试
      </button>
    </div>

    <!-- 空态 -->
    <div
      v-if="!loading && !generatingSkeleton && !personas.length && !error"
      class="rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 p-16 text-center"
    >
      <UserGroupIcon class="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
      <p class="text-gray-500 dark:text-gray-400 text-sm">
        还没有用户画像
      </p>
      <button
        class="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
        @click="showGenerateModal = true"
      >
        <SparklesIcon class="w-4 h-4" />让 AI 帮你生成 4 个画像
      </button>
    </div>

    <!-- 骨架屏(初次加载或生成中) -->
    <div
      v-if="(loading && !personas.length) || generatingSkeleton"
      class="grid grid-cols-1 md:grid-cols-2 gap-4"
    >
      <div
        v-for="i in 4"
        :key="i"
        class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-72"
      />
    </div>

    <!-- 画像卡片网格 -->
    <div
      v-else-if="personas.length"
      class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6"
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

    <!-- 覆盖度面板 -->
    <PersonaCoverageBar
      v-if="personas.length"
      :personas="personas"
    />

    <GeneratePersonaModal
      v-if="showGenerateModal"
      :project-id="projectId"
      @close="showGenerateModal = false"
      @completed="handleGenerated"
    />
  </div>
</template>
