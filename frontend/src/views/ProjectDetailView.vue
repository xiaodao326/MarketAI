<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const projectId = route.params.id
</script>

<template>
  <div>
    <h2 class="text-2xl font-semibold text-gray-900 mb-6">项目详情 #{{ projectId }}</h2>
    <div class="rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
      <p class="text-gray-500">项目详情功能即将上线，敬请期待</p>
    </div>
  </div>
</template>
