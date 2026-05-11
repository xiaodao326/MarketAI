<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<template>
  <div>
    <h2 class="text-2xl font-semibold text-gray-900">
      欢迎，{{ userStore.userInfo?.nickname || '用户' }}
    </h2>
    <p class="text-gray-500 mt-1">AI 驱动的市场需求分析平台</p>

    <div class="mt-12 flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
      <svg class="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m5.231 13.481L15 17.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v16.5c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">创建第一个分析项目</h3>
      <p class="text-sm text-gray-500 mb-6 max-w-md">
        选择一个市场关键词，AI 将自动分析市场趋势、竞争格局和目标用户画像，生成深度洞察报告。
      </p>
      <button
        @click="router.push('/projects')"
        class="px-6 py-2.5 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors"
      >
        开始新项目
      </button>
    </div>
  </div>
</template>
