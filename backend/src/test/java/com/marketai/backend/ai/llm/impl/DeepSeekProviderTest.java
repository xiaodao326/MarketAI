package com.marketai.backend.ai.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.ai.llm.dto.Message;
import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekProvider 单元测试")
class DeepSeekProviderTest {

    private MockWebServer mockServer;
    private DeepSeekProvider provider;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** 短超时 OkHttpClient, 避免测试等待真实超时 */
    private static final OkHttpClient SHORT_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        // retryDelayMs=10ms 避免重试等待, 只验证重试次数和最终行为
        provider = new DeepSeekProvider(
                mockServer.url("/v1").toString(),
                "test-api-key",
                "deepseek-chat",
                SHORT_CLIENT,
                objectMapper,
                10L
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // ── 成功场景 ──────────────────────────────────────────────

    @Nested
    @DisplayName("成功调用")
    class SuccessCase {

        @Test
        @DisplayName("正常对话 — 返回 content 和 token 统计")
        void shouldReturnContentAndTokens() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "id": "req-001",
                              "model": "deepseek-chat",
                              "choices": [{"message": {"role": "assistant", "content": "你好！"}, "finish_reason": "stop"}],
                              "usage": {"prompt_tokens": 12, "completion_tokens": 4, "total_tokens": 16}
                            }
                            """));

            ChatRequest request = ChatRequest.of("system: 你是助手", "Hello");
            request.setMessages(List.of(Message.user("Hello")));
            ChatResponse response = provider.chat(request);

            assertThat(response.getContent()).isEqualTo("你好！");
            assertThat(response.getPromptTokens()).isEqualTo(12);
            assertThat(response.getCompletionTokens()).isEqualTo(4);
            assertThat(response.getTotalTokens()).isEqualTo(16);
            assertThat(response.getModel()).isEqualTo("deepseek-chat");
            assertThat(response.getRequestId()).isEqualTo("req-001");
            assertThat(response.getLatencyMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("JSON Mode — content 直接可解析")
        void shouldReturnValidJsonInJsonMode() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "id": "req-002",
                              "model": "deepseek-chat",
                              "choices": [{"message": {"role": "assistant", "content": "{\\"score\\": 85}"}, "finish_reason": "stop"}],
                              "usage": {"prompt_tokens": 20, "completion_tokens": 8, "total_tokens": 28}
                            }
                            """));

            ChatRequest request = new ChatRequest();
            request.setMessages(List.of(Message.user("score this")));
            request.setJsonMode(true);

            ChatResponse response = provider.chat(request);

            assertThat(response.getContent()).contains("score");
        }
    }

    // ── 限流场景 ──────────────────────────────────────────────

    @Nested
    @DisplayName("限流 (HTTP 429)")
    class RateLimitCase {

        @Test
        @DisplayName("AI 提供商返回 429 — 立即抛出 TooManyRequestsException, 不重试")
        void shouldThrowImmediatelyOn429() {
            // 只入队 1 条, 验证不会重试 (否则第二次请求没有 mock 响应会 hang)
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"error": {"message": "Rate limit exceeded", "type": "requests_per_min_limit_reached"}}
                            """));

            ChatRequest request = ChatRequest.of("test");
            assertThatThrownBy(() -> provider.chat(request))
                    .isInstanceOf(TooManyRequestsException.class)
                    .hasMessageContaining("限流");

            // 验证只发出了 1 次请求 (无重试)
            assertThat(mockServer.getRequestCount()).isEqualTo(1);
        }
    }

    // ── 网络错误 / 超时场景 ──────────────────────────────────

    @Nested
    @DisplayName("网络错误与重试")
    class NetworkErrorCase {

        @Test
        @DisplayName("服务器断开连接 — 重试 3 次后抛出 RuntimeException")
        void shouldRetry3TimesOnNetworkError() {
            // 入队 3 条断开响应, 对应 3 次重试
            for (int i = 0; i < 3; i++) {
                mockServer.enqueue(new MockResponse()
                        .setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
            }

            ChatRequest request = ChatRequest.of("test");
            assertThatThrownBy(() -> provider.chat(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("已重试");

            assertThat(mockServer.getRequestCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("首次失败后重试成功 — 返回正确响应")
        void shouldSucceedAfterOneRetry() {
            // 第 1 次失败
            mockServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
            // 第 2 次成功
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "id": "req-retry",
                              "model": "deepseek-chat",
                              "choices": [{"message": {"role": "assistant", "content": "重试成功"}, "finish_reason": "stop"}],
                              "usage": {"prompt_tokens": 5, "completion_tokens": 3, "total_tokens": 8}
                            }
                            """));

            ChatResponse response = provider.chat(ChatRequest.of("test"));
            assertThat(response.getContent()).isEqualTo("重试成功");
            assertThat(mockServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("服务端 5xx — 重试 3 次后抛出 RuntimeException")
        void shouldRetryOn5xx() {
            for (int i = 0; i < 3; i++) {
                mockServer.enqueue(new MockResponse()
                        .setResponseCode(500)
                        .setBody("Internal Server Error"));
            }

            assertThatThrownBy(() -> provider.chat(ChatRequest.of("test")))
                    .isInstanceOf(RuntimeException.class);

            assertThat(mockServer.getRequestCount()).isEqualTo(3);
        }
    }
}
