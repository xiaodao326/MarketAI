<script setup lang="ts">
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import {
  HomeIcon,
  FolderIcon,
  Cog6ToothIcon,
  ChevronLeftIcon,
  UserCircleIcon,
  ArrowRightOnRectangleIcon,
} from '@heroicons/vue/24/outline'
import { Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const menuItems = [
  { path: '/dashboard', label: '仪表盘', icon: HomeIcon },
  { path: '/projects', label: '项目', icon: FolderIcon },
  { path: '/settings', label: '设置', icon: Cog6ToothIcon },
]

function isActive(path: string) {
  return route.path.startsWith(path)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen flex">
    <!-- 侧边栏 -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-30 flex flex-col bg-white border-r border-gray-200 transition-all duration-300',
        appStore.sidebarCollapsed ? 'w-16' : 'w-60',
      ]"
    >
      <!-- Logo -->
      <div class="h-16 flex items-center justify-center border-b border-gray-200 px-4">
        <span v-if="!appStore.sidebarCollapsed" class="text-xl font-bold text-primary-500">MarketAI</span>
        <span v-else class="text-lg font-bold text-primary-500">M</span>
      </div>

      <!-- 导航菜单 -->
      <nav class="flex-1 py-4 space-y-1 px-2">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors',
            isActive(item.path)
              ? 'bg-primary-50 text-primary-600 font-medium'
              : 'text-gray-600 hover:bg-gray-100',
          ]"
        >
          <component :is="item.icon" class="w-5 h-5 flex-shrink-0" />
          <span v-if="!appStore.sidebarCollapsed">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- 折叠按钮 -->
      <div class="border-t border-gray-200 p-2">
        <button
          @click="appStore.toggleSidebar()"
          class="w-full flex items-center justify-center p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
        >
          <ChevronLeftIcon
            :class="['w-5 h-5 transition-transform', appStore.sidebarCollapsed && 'rotate-180']"
          />
        </button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div :class="['flex-1 flex flex-col min-h-screen transition-all duration-300', appStore.sidebarCollapsed ? 'ml-16' : 'ml-60']">
      <!-- 顶部 Header -->
      <header class="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-20">
        <span class="text-sm text-gray-400">项目切换器（即将上线）</span>

        <!-- 用户下拉菜单 -->
        <Menu as="div" class="relative">
          <MenuButton class="flex items-center gap-2 p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
              <UserCircleIcon class="w-5 h-5 text-primary-600" />
            </div>
            <span class="text-sm text-gray-700 hidden sm:block">
              {{ userStore.userInfo?.nickname || '用户' }}
            </span>
          </MenuButton>
          <transition
            enter-active-class="transition duration-100 ease-out"
            enter-from-class="transform scale-95 opacity-0"
            enter-to-class="transform scale-100 opacity-100"
            leave-active-class="transition duration-75 ease-in"
            leave-from-class="transform scale-100 opacity-100"
            leave-to-class="transform scale-95 opacity-0"
          >
            <MenuItems class="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 focus:outline-none">
              <div class="px-4 py-3 border-b border-gray-100">
                <p class="text-sm font-medium text-gray-900">{{ userStore.userInfo?.nickname }}</p>
                <p class="text-xs text-gray-500 truncate">{{ userStore.userInfo?.email }}</p>
              </div>
              <MenuItem v-slot="{ active }">
                <button
                  @click="handleLogout()"
                  :class="['w-full flex items-center gap-2 px-4 py-2 text-sm', active ? 'bg-gray-50 text-gray-900' : 'text-gray-700']"
                >
                  <ArrowRightOnRectangleIcon class="w-4 h-4" />
                  退出登录
                </button>
              </MenuItem>
            </MenuItems>
          </transition>
        </Menu>
      </header>

      <!-- 页面内容 -->
      <main class="flex-1 p-6 bg-gray-50">
        <RouterView />
      </main>
    </div>
  </div>
</template>
