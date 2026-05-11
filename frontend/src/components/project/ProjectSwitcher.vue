<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { FolderIcon } from '@heroicons/vue/24/outline'

const router = useRouter()
const projectStore = useProjectStore()
const open = ref(false)

function toggle() { open.value = !open.value }

function selectProject(id: number) {
  const project = projectStore.projectList.find(p => p.id === id)
  if (project) {
    projectStore.switchProject(project)
    router.push(`/projects/${id}`)
  }
  open.value = false
}

onMounted(async () => {
  if (projectStore.projectList.length === 0) {
    await projectStore.fetchList({ size: 5 })
  }
})
</script>

<template>
  <div class="relative" :class="{ 'z-50': open }">
    <button @click="toggle" class="flex items-center gap-2 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
      <FolderIcon class="w-4 h-4 text-gray-400" />
      <span v-if="projectStore.currentProject" class="max-w-[120px] truncate">{{ projectStore.currentProject.name }}</span>
      <span v-else class="text-gray-400">选择项目</span>
      <svg class="w-3.5 h-3.5 text-gray-400 transition-transform" :class="{ 'rotate-180': open }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" /></svg>
    </button>

    <div v-if="open" class="absolute left-0 top-full mt-1 w-60 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-40">
      <div class="px-3 py-2 text-xs text-gray-400 font-medium">最近项目</div>
      <button
        v-for="project in projectStore.projectList"
        :key="project.id"
        @click="selectProject(project.id)"
        :class="[
          'w-full text-left px-3 py-2 text-sm transition-colors',
          projectStore.currentProject?.id === project.id ? 'bg-primary-50 text-primary-700' : 'text-gray-700 hover:bg-gray-50',
        ]"
      >
        <div class="truncate">{{ project.name }}</div>
        <div class="text-xs text-gray-400 mt-0.5">{{ project.industry }} · {{ project.keywords?.length || 0 }} 关键词</div>
      </button>
      <div v-if="projectStore.projectList.length === 0" class="px-3 py-4 text-center text-xs text-gray-400">
        暂无项目，请先创建
      </div>
      <div class="border-t border-gray-100 mt-1 pt-1">
        <button @click="router.push('/projects'); open = false" class="w-full text-left px-3 py-2 text-sm text-primary-600 hover:bg-primary-50 transition-colors">
          查看全部项目
        </button>
      </div>
    </div>

    <!-- Click outside mask -->
    <div v-if="open" class="fixed inset-0 z-30" @click="open = false" />
  </div>
</template>
