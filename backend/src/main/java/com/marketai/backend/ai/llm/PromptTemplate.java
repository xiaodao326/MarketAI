package com.marketai.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt 模板引擎
 * 模板文件存放在 classpath:prompts/*.txt, 使用 {{variable}} 占位符
 *
 * 为什么独立管理 Prompt 而不是硬编码在 Service 中?
 *   Prompt 是 AI 产品的核心资产, 需要频繁迭代调试, 独立文件便于：
 *   1. 非工程师也能修改 Prompt (产品/运营)
 *   2. 版本控制清晰 (git diff 可以看 prompt 变更历史)
 *   3. 不同语言/地区可以有不同 prompt 文件
 */
@Slf4j
public class PromptTemplate {

    private final String template;

    private PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * 从 classpath:prompts/{name}.txt 加载模板
     * @param name 模板文件名 (不含 .txt 后缀)
     */
    public static PromptTemplate load(String name) {
        String path = "/prompts/" + name + ".txt";
        try (InputStream is = PromptTemplate.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Prompt 模板不存在: " + path);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new PromptTemplate(content);
        } catch (IOException e) {
            throw new RuntimeException("加载 Prompt 模板失败: " + path, e);
        }
    }

    /** 用字符串直接构建模板 (用于测试或动态模板) */
    public static PromptTemplate of(String template) {
        return new PromptTemplate(template);
    }

    /**
     * 渲染模板: 将 {{key}} 替换为 variables 中对应的值
     * 未找到的占位符原样保留 (便于调试定位缺失变量)
     */
    public String render(Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /** 单个变量快捷渲染 */
    public String render(String key, Object value) {
        return render(Map.of(key, value));
    }

    public String getTemplate() {
        return template;
    }
}
