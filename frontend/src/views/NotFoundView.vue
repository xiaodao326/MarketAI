<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import { ArrowLeftIcon, HomeIcon } from '@heroicons/vue/24/outline'

const router = useRouter()
</script>

<template>
  <div class="flex flex-col items-center justify-center text-center py-20 px-4 min-h-[60vh] relative overflow-hidden">
    <!-- 装饰光斑 -->
    <div class="pointer-events-none absolute top-1/4 left-1/3 w-96 h-96 rounded-full bg-brand-500/10 blur-3xl"></div>
    <div class="pointer-events-none absolute bottom-1/4 right-1/3 w-96 h-96 rounded-full bg-accent-500/10 blur-3xl"></div>

    <div class="relative">
      <h1 class="text-[140px] font-black leading-none tracking-tighter bg-brand-gradient bg-clip-text text-transparent select-none">
        404
      </h1>
      <p class="text-xl font-semibold text-neutral-900 dark:text-neutral-100 mt-2">页面不存在</p>
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-2 max-w-md">
        抱歉,你访问的页面已被移动、删除,或链接拼写错误。
      </p>

      <div class="flex items-center justify-center gap-2 mt-8">
        <AppButton variant="secondary" @click="router.back()">
          <ArrowLeftIcon class="w-4 h-4" />返回上一页
        </AppButton>
        <AppButton variant="gradient" @click="router.push('/dashboard')">
          <HomeIcon class="w-4 h-4" />回到工作台
        </AppButton>
      </div>
    </div>
  </div>
</template>
