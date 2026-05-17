<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useProjectStore } from '@/stores/project'
import {
  HomeIcon,
  FolderIcon,
  ChartBarIcon,
  LightBulbIcon,
  UserGroupIcon,
  BuildingOffice2Icon,
  ChevronLeftIcon,
  ChevronDoubleRightIcon,
  ArrowRightOnRectangleIcon,
  UserCircleIcon,
  BellIcon,
  Cog6ToothIcon,
  Bars3Icon,
} from '@heroicons/vue/24/outline'
import { Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const projectStore = useProjectStore()

interface NavItem {
  path: string
  label: string
  icon: typeof HomeIcon
  match?: (path: string) => boolean
}

const primaryNav: NavItem[] = [
  { path: '/dashboard', label: '工作台', icon: HomeIcon },
  { path: '/projects', label: '项目', icon: FolderIcon, match: (p) => p === '/projects' || /^\/projects\/?$/.test(p) },
]

// 当前项目的子模块导航 — 仅在 /projects/:id/* 下显示
const projectNav = computed(() => {
  const id = route.params.id
  if (!id) return []
  return [
    { path: `/projects/${id}`,             label: '项目概览', icon: FolderIcon },
    { path: `/projects/${id}/dashboard`,   label: '趋势仪表盘', icon: ChartBarIcon },
    { path: `/projects/${id}/insights`,    label: 'AI 需求洞察', icon: LightBulbIcon },
    { path: `/projects/${id}/personas`,    label: '用户画像', icon: UserGroupIcon },
    { path: `/projects/${id}/competitors`, label: '竞品分析', icon: BuildingOffice2Icon },
  ]
})

function isActive(item: NavItem) {
  if (item.match) return item.match(route.path)
  return route.path === item.path || route.path.startsWith(item.path + '/')
}

function isProjectNavActive(path: string) {
  return route.path === path
}

const userInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.email || 'U'
  return name.charAt(0).toUpperCase()
})

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}

function toggleSidebar() {
  appStore.toggleSidebar()
}
</script>

<template>
  <div class="min-h-screen flex bg-[color:var(--color-canvas)] text-[color:var(--color-text-primary)]">
    <!-- ====== 侧边栏 ====== -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-30 flex flex-col bg-[color:var(--color-surface)] border-r border-[color:var(--color-border)]',
        'transition-[width] duration-200 ease-out',
        appStore.sidebarCollapsed ? 'w-[68px]' : 'w-[232px]',
      ]"
    >
      <!-- Logo -->
      <div class="h-16 flex items-center px-4 border-b border-[color:var(--color-border)]">
        <div class="flex items-center gap-2.5 min-w-0">
          <div
            class="w-8 h-8 rounded-lg bg-brand-gradient flex items-center justify-center flex-shrink-0 shadow-glow"
          >
            <span class="text-white font-bold text-sm">M</span>
          </div>
          <div v-if="!appStore.sidebarCollapsed" class="min-w-0">
            <div class="text-[15px] font-semibold leading-tight tracking-tight">MarketAI</div>
            <div class="text-[10px] text-neutral-400 leading-tight mt-0.5">市场需求 AI 分析</div>
          </div>
        </div>
      </div>

      <!-- 主导航 -->
      <nav class="flex-1 overflow-y-auto py-4 px-3 space-y-0.5">
        <div v-if="!appStore.sidebarCollapsed" class="px-2 mb-1.5 text-[10px] uppercase tracking-wider text-neutral-400 font-medium">
          导航
        </div>
        <router-link
          v-for="item in primaryNav"
          :key="item.path"
          :to="item.path"
          :class="[
            'group flex items-center gap-2.5 px-2.5 py-2 rounded-md text-sm transition-all',
            isActive(item)
              ? 'bg-brand-50 dark:bg-brand-500/10 text-brand-700 dark:text-brand-300 font-medium'
              : 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800',
          ]"
        >
          <component :is="item.icon" class="w-[18px] h-[18px] flex-shrink-0" />
          <span v-if="!appStore.sidebarCollapsed">{{ item.label }}</span>
        </router-link>

        <!-- 项目子导航(动态) -->
        <template v-if="projectNav.length">
          <div v-if="!appStore.sidebarCollapsed" class="px-2 mt-5 mb-1.5 text-[10px] uppercase tracking-wider text-neutral-400 font-medium flex items-center justify-between">
            <span>当前项目</span>
          </div>
          <div v-if="!appStore.sidebarCollapsed && projectStore.currentProject" class="px-2.5 mb-2 text-[11px] text-brand-600 dark:text-brand-400 truncate font-medium">
            {{ projectStore.currentProject.name }}
          </div>
          <router-link
            v-for="item in projectNav"
            :key="item.path"
            :to="item.path"
            :class="[
              'group flex items-center gap-2.5 px-2.5 py-2 rounded-md text-sm transition-all',
              isProjectNavActive(item.path)
                ? 'bg-brand-50 dark:bg-brand-500/10 text-brand-700 dark:text-brand-300 font-medium'
                : 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800',
            ]"
          >
            <component :is="item.icon" class="w-[18px] h-[18px] flex-shrink-0" />
            <span v-if="!appStore.sidebarCollapsed" class="truncate">{{ item.label }}</span>
          </router-link>
        </template>
      </nav>

      <!-- 底部:折叠 / 设置 -->
      <div class="border-t border-[color:var(--color-border)] p-2 flex items-center gap-1" :class="appStore.sidebarCollapsed ? 'flex-col' : ''">
        <button
          class="flex-1 inline-flex items-center justify-center h-9 rounded-md text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
          @click="toggleSidebar"
          :title="appStore.sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
        >
          <ChevronDoubleRightIcon v-if="appStore.sidebarCollapsed" class="w-4 h-4" />
          <ChevronLeftIcon v-else class="w-4 h-4" />
        </button>
      </div>
    </aside>

    <!-- ====== 主内容区 ====== -->
    <div
      :class="[
        'flex-1 flex flex-col min-h-screen transition-[margin] duration-200 ease-out',
        appStore.sidebarCollapsed ? 'ml-[68px]' : 'ml-[232px]',
      ]"
    >
      <!-- 顶部 Header -->
      <header class="h-16 sticky top-0 z-20 bg-[color:var(--color-surface)]/85 backdrop-blur-md border-b border-[color:var(--color-border)] px-6 flex items-center justify-between gap-4">
        <!-- 左:折叠按钮(移动) + 面包屑 -->
        <div class="flex items-center gap-3 min-w-0 flex-1">
          <button
            class="lg:hidden p-1.5 rounded-md text-neutral-500 hover:bg-neutral-100 dark:hover:bg-neutral-800"
            @click="toggleSidebar"
          >
            <Bars3Icon class="w-5 h-5" />
          </button>
          <Breadcrumb class="min-w-0" />
        </div>

        <!-- 右:工具栏 -->
        <div class="flex items-center gap-1">
          <ThemeToggle />
          <button
            type="button"
            class="hidden sm:flex items-center justify-center w-9 h-9 rounded-md text-neutral-500 dark:text-neutral-400 hover:text-brand-600 dark:hover:text-brand-300 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-all relative"
            title="通知"
          >
            <BellIcon class="w-5 h-5" />
          </button>

          <!-- 分隔 -->
          <div class="w-px h-6 bg-[color:var(--color-border)] mx-2"></div>

          <!-- 用户菜单 -->
          <Menu as="div" class="relative">
            <MenuButton class="flex items-center gap-2 pl-1 pr-2 py-1 rounded-md hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors">
              <div class="w-7 h-7 rounded-md bg-brand-gradient flex items-center justify-center text-white text-xs font-semibold">
                {{ userInitial }}
              </div>
              <span class="text-sm text-neutral-700 dark:text-neutral-200 hidden sm:block max-w-[100px] truncate">
                {{ userStore.userInfo?.nickname || '用户' }}
              </span>
            </MenuButton>
            <transition
              enter-active-class="transition duration-150 ease-out"
              enter-from-class="transform scale-95 opacity-0"
              enter-to-class="transform scale-100 opacity-100"
              leave-active-class="transition duration-100 ease-in"
              leave-from-class="transform scale-100 opacity-100"
              leave-to-class="transform scale-95 opacity-0"
            >
              <MenuItems class="absolute right-0 mt-2 w-56 bg-[color:var(--color-surface-elevated)] rounded-lg shadow-overlay border border-[color:var(--color-border)] py-1.5 focus:outline-none origin-top-right z-50">
                <div class="px-3 py-2.5 border-b border-[color:var(--color-border)] mb-1">
                  <p class="text-sm font-medium text-neutral-900 dark:text-neutral-100 truncate">{{ userStore.userInfo?.nickname }}</p>
                  <p class="text-xs text-neutral-500 truncate mt-0.5">{{ userStore.userInfo?.email }}</p>
                </div>
                <MenuItem v-slot="{ active }">
                  <button
                    :class="['w-full flex items-center gap-2 px-3 py-2 text-sm rounded-md mx-1 transition-colors', active ? 'bg-neutral-100 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-100' : 'text-neutral-700 dark:text-neutral-300']"
                  >
                    <UserCircleIcon class="w-4 h-4" />个人信息
                  </button>
                </MenuItem>
                <MenuItem v-slot="{ active }">
                  <button
                    :class="['w-full flex items-center gap-2 px-3 py-2 text-sm rounded-md mx-1 transition-colors', active ? 'bg-neutral-100 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-100' : 'text-neutral-700 dark:text-neutral-300']"
                  >
                    <Cog6ToothIcon class="w-4 h-4" />偏好设置
                  </button>
                </MenuItem>
                <div class="my-1 border-t border-[color:var(--color-border)]"></div>
                <MenuItem v-slot="{ active }">
                  <button
                    @click="handleLogout"
                    :class="['w-full flex items-center gap-2 px-3 py-2 text-sm rounded-md mx-1 transition-colors', active ? 'bg-red-50 text-red-600 dark:bg-red-500/10 dark:text-red-400' : 'text-neutral-700 dark:text-neutral-300']"
                  >
                    <ArrowRightOnRectangleIcon class="w-4 h-4" />退出登录
                  </button>
                </MenuItem>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </header>

      <!-- 页面内容(含路由切换过渡) -->
      <main class="flex-1 px-6 py-6 lg:px-8 lg:py-8">
        <router-view v-slot="{ Component, route: r }">
          <transition name="page" mode="out-in">
            <component :is="Component" :key="r.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>
