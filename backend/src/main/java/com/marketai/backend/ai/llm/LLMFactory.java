package com.marketai.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM 工厂 — 工厂模式
 *
 * 为什么用工厂模式而非直接注入 LLMProvider?
 *   调用方不需要知道具体使用哪个模型, 只需要 factory.getProvider() 拿到当前配置的 Provider。
 *   当需要 A/B 测试或切换模型时, 只需改配置而不是改所有调用点。
 *
 * Spring 自动收集所有 LLMProvider 实现注入到 providerMap, 新增模型无需修改此类。
 */
@Slf4j
@Component
public class LLMFactory {

    /** 当前激活的提供商, volatile 保证多线程可见性 */
    private volatile String activeProvider;

    /** name -> LLMProvider 映射 */
    private final Map<String, LLMProvider> providerMap;

    public LLMFactory(
            List<LLMProvider> providers,
            @Value("${marketai.ai.provider:deepseek}") String defaultProvider) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(LLMProvider::getName, p -> p));
        this.activeProvider = defaultProvider;
        log.info("[LLMFactory] 已注册 {} 个 Provider: {}, 默认使用: {}",
                providerMap.size(), providerMap.keySet(), defaultProvider);
    }

    /** 获取当前激活的 Provider */
    public LLMProvider getProvider() {
        LLMProvider p = providerMap.get(activeProvider);
        if (p == null) {
            log.warn("[LLMFactory] 提供商 '{}' 未找到, 回退到 deepseek", activeProvider);
            p = providerMap.get("deepseek");
        }
        return p;
    }

    /**
     * 运行时动态切换提供商
     * @param name deepseek / qwen / glm
     */
    public void switchProvider(String name) {
        if (!providerMap.containsKey(name)) {
            throw new IllegalArgumentException("未知提供商: " + name + ", 可选: " + providerMap.keySet());
        }
        log.info("[LLMFactory] 切换 Provider: {} -> {}", activeProvider, name);
        this.activeProvider = name;
    }

    /** 返回所有已注册提供商的名称列表 */
    public java.util.Set<String> availableProviders() {
        return providerMap.keySet();
    }

    public String getActiveProvider() {
        return activeProvider;
    }
}
