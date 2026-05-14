# 04 — AI Prompt 工程

> 状态: Phase 1 骨架 | 最后更新: 2026-05-14

## Prompt 模板管理

所有 Prompt 模板定义在 `com.marketai.backend.ai.llm.PromptTemplate` 中, 采用 `Message.system()` + `Message.user()` 构建对话。

## 模板设计原则

1. **角色设定**: 每个 prompt 开头设定 AI 角色 (如 "你是一位资深市场分析师")
2. **JSON Schema 约束**: 明确要求输出合法 JSON, 提供字段说明
3. **示例驱动**: 给出输出示例, 减少格式偏差
4. **防幻觉**: 要求基于用户输入数据, 不确定的字段标为 null 或 "需要更多信息"

## 各模块 Prompt 概要

### 需求洞察 (`InsightAnalyzer`)

- **输入**: 项目关键词、行业、目标市场、产品描述, 采集的趋势数据
- **输出**: `InsightResult` JSON
  - `marketFitScore` (0-100)
  - `dimensions` (需求强度/竞争饱和度/增长潜力/进入壁垒)
  - `painPoints` (痛点列表, 含严重度)
  - `opportunities` (机会矩阵)
  - `risks` (风险评估)
  - `actions` (行动建议, 按优先级排序)
- **调优参数**: temperature=0.3 (结构化输出), maxTokens=4000

### 用户画像 (`PersonaGenerator`)

- **输入**: 项目关键词、产品描述、目标市场
- **输出**: `List<PersonaResult>` JSON (3-5 个画像)
  - 每个画像含: 姓名、角色、年龄区间、市场占比、核心目标、痛点、决策参数
- **调优参数**: temperature=0.5, maxTokens=3000

### 竞品分析 (`CompetitorAnalyzer`)

- **输入**: 项目关键词、竞品名单、产品描述
- **输出**: `CompetitorAnalysisResult` JSON
  - `competitors` (逐竞品分析: 类型/评分/定价/融资/威胁等级/优劣势)
  - `featureMatrix` (功能对比矩阵 + 机会标记)
  - `differentiationInsights` (差异化建议)
- **调优参数**: temperature=0.3, maxTokens=4000

## Provider 切换

```yaml
# application.yml
marketai:
  ai:
    provider: deepseek  # deepseek / qwen / glm
```

API 层支持运行时切换: `POST /api/v1/llm/switch?provider=qwen`

## 调优记录

| 日期 | 问题 | 调整 | 效果 |
|------|------|------|------|
| 2026-05-11 | JSON 格式偶尔不合法 | prompt 增加 "必须输出合法 JSON, 不要输出 markdown 代码块" | 合法性从 ~85% 提升至 ~98% |
| 2026-05-12 | 竞品分析评分偏差大 | prompt 增加评分锚定说明 (5=行业标杆, 3=中等, 1=低) | 评分一致性提升 |
| — | (待补充) | — | — |

## JSON Mode

DeepSeek/千问/GLM 均支持 JSON Mode (`response_format: { type: "json_object" }`), 通过 `ChatRequest.jsonMode = true` 开启。当前洞察分析启用 JSON Mode, 其他场景默认关闭但 prompt 中已要求 JSON 输出。

## Token 消耗预估

| 任务 | 输入 tokens | 输出 tokens | 合计 |
|------|-------------|-------------|------|
| 需求洞察 (standard) | ~2000 | ~2500 | ~4500 |
| 用户画像 (4个) | ~1500 | ~2000 | ~3500 |
| 竞品分析 (3竞品) | ~2000 | ~3000 | ~5000 |

使用 DeepSeek 价格 (约 ¥0.001/1K tokens), 单次分析成本 < ¥0.01。