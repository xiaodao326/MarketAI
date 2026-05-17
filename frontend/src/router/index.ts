import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', layout: 'auth', guest: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', layout: 'auth', guest: true },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: '工作台', requiresAuth: true, breadcrumb: ['工作台'] },
  },
  {
    path: '/projects',
    name: 'projects',
    component: () => import('@/views/ProjectsView.vue'),
    meta: { title: '项目列表', requiresAuth: true, breadcrumb: ['项目'] },
  },
  {
    path: '/projects/:id',
    name: 'project-detail',
    component: () => import('@/views/ProjectDetailView.vue'),
    meta: { title: '项目详情', requiresAuth: true, breadcrumb: ['项目', ':project'] },
  },
  {
    path: '/projects/:id/dashboard',
    name: 'project-dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { title: '趋势仪表盘', requiresAuth: true, breadcrumb: ['项目', ':project', '趋势仪表盘'] },
  },
  {
    path: '/projects/:id/insights',
    name: 'project-insights',
    component: () => import('@/views/insights/InsightView.vue'),
    meta: { title: 'AI 需求洞察', requiresAuth: true, breadcrumb: ['项目', ':project', 'AI 需求洞察'] },
  },
  {
    path: '/projects/:id/personas',
    name: 'project-personas',
    component: () => import('@/views/personas/PersonaView.vue'),
    meta: { title: '用户画像', requiresAuth: true, breadcrumb: ['项目', ':project', '用户画像'] },
  },
  {
    path: '/projects/:id/competitors',
    name: 'project-competitors',
    component: () => import('@/views/competitors/CompetitorView.vue'),
    meta: { title: '竞品分析', requiresAuth: true, breadcrumb: ['项目', ':project', '竞品分析'] },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// 路由守卫：认证检查
router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || 'MarketAI'} · MarketAI`

  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'dashboard' })
  } else {
    next()
  }
})

export default router
