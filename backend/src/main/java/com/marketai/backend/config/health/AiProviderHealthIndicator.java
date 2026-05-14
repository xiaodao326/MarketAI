package com.marketai.backend.config.health;

import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.LLMProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AiProviderHealthIndicator implements HealthIndicator {

    private final LLMFactory llmFactory;

    public AiProviderHealthIndicator(LLMFactory llmFactory) {
        this.llmFactory = llmFactory;
    }

    @Override
    public Health health() {
        try {
            LLMProvider provider = llmFactory.getProvider();
            String name = llmFactory.getActiveProvider();
            if (provider != null) {
                return Health.up()
                        .withDetail("provider", name)
                        .withDetail("status", "available")
                        .build();
            }
            return Health.unknown()
                    .withDetail("provider", "none")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", llmFactory.getActiveProvider())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}