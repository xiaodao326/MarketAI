<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-12">
    <div class="w-full max-w-5xl flex bg-white rounded-lg shadow-sm overflow-hidden">
      <!-- 左侧：产品介绍 -->
      <div class="hidden lg:flex lg:w-1/2 bg-primary-500 p-12 flex-col justify-center">
        <h1 class="text-4xl font-bold text-white mb-4">MarketAI</h1>
        <p class="text-primary-100 text-lg leading-relaxed">
          AI 驱动的市场需求分析平台，帮助创业者和产品团队快速洞察市场机会，降低决策风险。
        </p>
        <div class="mt-10 space-y-4">
          <div class="flex items-center text-white">
            <span class="w-8 h-8 rounded-full bg-primary-400 flex items-center justify-center mr-3 text-sm font-medium">1</span>
            <span>创建分析项目，输入目标市场关键词</span>
          </div>
          <div class="flex items-center text-white">
            <span class="w-8 h-8 rounded-full bg-primary-400 flex items-center justify-center mr-3 text-sm font-medium">2</span>
            <span>AI 自动抓取市场数据与趋势</span>
          </div>
          <div class="flex items-center text-white">
            <span class="w-8 h-8 rounded-full bg-primary-400 flex items-center justify-center mr-3 text-sm font-medium">3</span>
            <span>获得深度洞察报告与竞品分析</span>
          </div>
        </div>
      </div>
      <!-- 右侧：表单区域 -->
      <div class="w-full lg:w-1/2 p-8 sm:p-12">
        <div class="lg:hidden mb-8 text-center">
          <h1 class="text-2xl font-bold text-primary-500">MarketAI</h1>
          <p class="text-sm text-gray-500 mt-1">AI 驱动的市场需求分析平台</p>
        </div>
        <RouterView />
      </div>
    </div>
  </div>
</template>
