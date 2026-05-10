<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <header class="h-16 bg-white border-b border-gray-200 flex items-center px-6">
      <h1 class="text-xl font-bold text-primary-600">MarketAI</h1>
      <nav class="ml-auto flex items-center gap-4">
        <!-- 导航占位 -->
      </nav>
    </header>
    <div class="flex-1 flex">
      <aside class="w-64 bg-white border-r border-gray-200 p-4">
        <!-- 侧边栏占位 -->
      </aside>
      <main class="flex-1 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>