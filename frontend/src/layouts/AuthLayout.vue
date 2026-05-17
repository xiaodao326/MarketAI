<script setup lang="ts">
import { RouterView } from 'vue-router'
import { SparklesIcon, ChartBarIcon, LightBulbIcon, UserGroupIcon } from '@heroicons/vue/24/outline'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const features = [
  { icon: ChartBarIcon,  label: '趋势仪表盘', desc: '搜索热度 × 社媒讨论 × 情感倾向 多维聚合' },
  { icon: LightBulbIcon, label: 'AI 需求洞察', desc: '一键生成痛点、机会、风险与行动建议' },
  { icon: UserGroupIcon, label: '用户画像生成', desc: 'LLM 自动产出 3–5 个目标画像与覆盖度' },
]
</script>

<template>
  <div class="min-h-screen w-full bg-[color:var(--color-canvas)] text-[color:var(--color-text-primary)] relative overflow-hidden">
    <!-- 顶部主题切换 -->
    <div class="absolute top-4 right-4 z-20">
      <ThemeToggle />
    </div>

    <!-- 背景装饰:网格 + 光斑 -->
    <div class="pointer-events-none absolute inset-0 bg-mesh [background-size:48px_48px] opacity-40 dark:opacity-20"></div>
    <div class="pointer-events-none absolute -top-32 -left-32 w-[520px] h-[520px] rounded-full bg-brand-500/20 blur-[120px]"></div>
    <div class="pointer-events-none absolute -bottom-32 -right-32 w-[520px] h-[520px] rounded-full bg-accent-500/20 blur-[120px]"></div>

    <div class="relative z-10 min-h-screen flex items-center justify-center px-4 py-10">
      <div class="w-full max-w-6xl grid lg:grid-cols-[1.05fr_1fr] rounded-2xl shadow-overlay border border-[color:var(--color-border)] overflow-hidden bg-[color:var(--color-surface-elevated)]">
        <!-- 左:品牌区 -->
        <div class="hidden lg:flex relative p-12 flex-col justify-between text-white overflow-hidden bg-brand-gradient">
          <!-- 内部装饰 -->
          <div class="pointer-events-none absolute inset-0 bg-mesh [background-size:32px_32px] opacity-20"></div>
          <div class="pointer-events-none absolute -top-24 -right-24 w-72 h-72 rounded-full bg-white/10 blur-3xl"></div>

          <div class="relative">
            <div class="flex items-center gap-3 mb-10">
              <div class="w-11 h-11 rounded-xl bg-white/15 backdrop-blur flex items-center justify-center shadow-glow">
                <SparklesIcon class="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 class="text-2xl font-bold tracking-tight">MarketAI</h1>
                <p class="text-xs text-white/70 mt-0.5">AI 市场需求分析平台</p>
              </div>
            </div>

            <h2 class="text-[28px] leading-tight font-bold mb-3">
              让 AI 替你<br/>读懂市场,看见机会
            </h2>
            <p class="text-white/85 text-[15px] leading-relaxed max-w-md">
              整合搜索趋势、社媒讨论与竞品情报,自动产出可执行的市场洞察报告,
              帮助创业者与产品团队大幅降低决策成本。
            </p>
          </div>

          <div class="relative space-y-3 mt-10">
            <div
              v-for="f in features"
              :key="f.label"
              class="flex items-start gap-3 p-3 rounded-xl bg-white/10 backdrop-blur-sm border border-white/15 hover:bg-white/15 transition-colors"
            >
              <div class="w-9 h-9 rounded-lg bg-white/20 flex items-center justify-center flex-shrink-0">
                <component :is="f.icon" class="w-5 h-5 text-white" />
              </div>
              <div class="min-w-0">
                <div class="text-sm font-semibold">{{ f.label }}</div>
                <div class="text-xs text-white/75 mt-0.5">{{ f.desc }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右:表单区 -->
        <div class="bg-[color:var(--color-surface)] p-8 sm:p-10 lg:p-14 flex flex-col justify-center">
          <div class="lg:hidden mb-8 flex items-center gap-2.5">
            <div class="w-9 h-9 rounded-lg bg-brand-gradient flex items-center justify-center">
              <SparklesIcon class="w-5 h-5 text-white" />
            </div>
            <div>
              <div class="text-base font-bold">MarketAI</div>
              <div class="text-[11px] text-neutral-500">AI 市场需求分析平台</div>
            </div>
          </div>
          <RouterView />
        </div>
      </div>
    </div>
  </div>
</template>
