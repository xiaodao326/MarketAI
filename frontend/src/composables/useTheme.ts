import { ref, onMounted } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'marketai_theme'

const mode = ref<ThemeMode>('system')
const isDark = ref(false)

function resolveDark(m: ThemeMode): boolean {
  if (m === 'dark') return true
  if (m === 'light') return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyDark(dark: boolean) {
  const root = document.documentElement
  if (dark) {
    root.classList.add('dark')
    root.style.colorScheme = 'dark'
  } else {
    root.classList.remove('dark')
    root.style.colorScheme = 'light'
  }
  isDark.value = dark
}

/**
 * 全局主题管理 — 浅色 / 暗色 / 跟随系统三态。
 * - 通过 documentElement 上的 .dark class 触发 Tailwind darkMode: 'class'
 * - 通过 CSS 变量驱动 Element Plus 颜色,无需 EP 自带的 dark 类
 * - localStorage 持久化用户选择
 * - mode='system' 时监听 prefers-color-scheme 变化
 */
export function useTheme() {
  let mediaQuery: MediaQueryList | null = null

  const init = () => {
    const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    mode.value = stored ?? 'system'
    applyDark(resolveDark(mode.value))

    // 监听系统主题变化(仅 system 模式下生效)
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', (e) => {
      if (mode.value === 'system') applyDark(e.matches)
    })
  }

  const setMode = (m: ThemeMode) => {
    mode.value = m
    localStorage.setItem(STORAGE_KEY, m)
    applyDark(resolveDark(m))
  }

  const toggle = () => {
    // 三态循环:light → dark → system → light
    const next: ThemeMode = mode.value === 'light' ? 'dark' : mode.value === 'dark' ? 'system' : 'light'
    setMode(next)
  }

  onMounted(() => {
    if (!mediaQuery) init()
  })

  return { mode, isDark, setMode, toggle, init }
}

/** 在 main.ts 调用,确保首屏无 FOUC */
export function initTheme() {
  const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
  const m = stored ?? 'system'
  mode.value = m
  applyDark(resolveDark(m))
}
