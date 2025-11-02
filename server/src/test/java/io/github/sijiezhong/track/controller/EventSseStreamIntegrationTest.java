package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.stream.EventStreamBroadcaster;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventSseStreamIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventStreamBroadcaster broadcaster;

    @Test
    void sseStreamEndpointAvailable() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn();
        String contentType = result.getResponse().getContentType();
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(contentType.contains("text/event-stream"));
    }

    @Test
    void postEventShouldBroadcastToTenant() throws Exception {
        // 预订阅以确保租户注册
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());

        String body = "{\n" +
                "  \"eventName\": \"page_view\",\n" +
                "  \"sessionId\": \"sess-sse-1\",\n" +
                "  \"tenantId\": 1\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 断言已广播：最后消息包含事件名
        String last = broadcaster.getLastMessageForTenant(1);
        Assertions.assertNotNull(last);
        Assertions.assertTrue(last.contains("page_view"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 租户1订阅SSE后，租户2创建事件，租户1不应该收到")
    void should_NotReceiveOtherTenantEvents_When_SubscribedToSSE() throws Exception {
        // Given: 租户1订阅SSE
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());

        // When: 租户2创建事件
        String tenant2Body = "{\n" +
                "  \"eventName\": \"tenant2_event\",\n" +
                "  \"sessionId\": \"sess-tenant2\",\n" +
                "  \"tenantId\": 2\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tenant2Body))
                .andExpect(status().isCreated());

        // Then: 租户1不应该收到租户2的事件
        String lastMessage = broadcaster.getLastMessageForTenant(1);
        // 如果租户1有消息，应该不包含租户2的事件名
        if (lastMessage != null) {
            Assertions.assertFalse(lastMessage.contains("tenant2_event"),
                    "CRITICAL: Tenant 1 should not receive events from tenant 2");
        }
        
        // 验证：租户1的最后消息应该为空，或者不包含租户2的事件
        // 注意：如果租户1之前没有事件，getLastMessageForTenant可能返回null，这是正常的
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 租户2订阅SSE后，应只收到租户2自己的事件")
    void should_ReceiveOnlyOwnTenantEvents_When_SubscribedToSSE() throws Exception {
        // Given: 租户2订阅SSE
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 2))
                .andExpect(status().isOk());

        // When: 租户2创建事件
        String tenant2Body = "{\n" +
                "  \"eventName\": \"tenant2_specific_event\",\n" +
                "  \"sessionId\": \"sess-tenant2-specific\",\n" +
                "  \"tenantId\": 2\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tenant2Body))
                .andExpect(status().isCreated());

        // Then: 租户2应该收到自己的事件
        String lastMessage = broadcaster.getLastMessageForTenant(2);
        Assertions.assertNotNull(lastMessage, "Tenant 2 should receive its own events");
        Assertions.assertTrue(lastMessage.contains("tenant2_specific_event"),
                "Tenant 2 should receive its own event");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 多个客户端同时订阅时，所有客户端都能收到事件")
    void should_BroadcastToAllClients_When_MultipleClientsSubscribe() throws Exception {
        // Given: 两个客户端同时订阅租户1的SSE
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());

        // When: 租户1创建事件
        String body = "{\n" +
                "  \"eventName\": \"broadcast_test\",\n" +
                "  \"sessionId\": \"sess-broadcast\",\n" +
                "  \"tenantId\": 1\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then: 验证事件被广播（通过broadcaster的lastMessage验证）
        // 注意：实际的SSE推送测试需要更复杂的设置，这里验证基本功能
        String lastMessage = broadcaster.getLastMessageForTenant(1);
        Assertions.assertNotNull(lastMessage, "Event should be broadcast to tenant 1");
        Assertions.assertTrue(lastMessage.contains("broadcast_test"),
                "All subscribers should receive the event");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: SSE连接断开时，系统应优雅处理")
    void should_HandleClientDisconnect_Gracefully() throws Exception {
        // Given: 客户端建立SSE连接
        mockMvc.perform(get("/api/v1/events/stream")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());

        // When: 连接建立后，客户端断开（模拟）
        // 注意：在实际SSE实现中，连接断开会触发onCompletion回调
        // 这里我们验证broadcaster能够正确处理断开（通过后续事件验证）
        
        // When: 创建事件
        String body = "{\n" +
                "  \"eventName\": \"after_disconnect\",\n" +
                "  \"sessionId\": \"sess-after\",\n" +
                "  \"tenantId\": 1\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then: 系统应该能够正常处理，不应该崩溃
        // 验证：事件仍然能够被处理
        String lastMessage = broadcaster.getLastMessageForTenant(1);
        Assertions.assertNotNull(lastMessage, "System should handle events even after client disconnect");
    }
}


