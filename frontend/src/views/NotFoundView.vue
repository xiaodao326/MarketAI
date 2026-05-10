<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen gap-4">
    <h1 class="text-6xl font-bold text-gray-300">404</h1>
    <p class="text-gray-500">页面不存在</p>
    <button
      class="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition"
      @click="router.push('/')"
    >
      返回首页
    </button>
  </div>
</template>