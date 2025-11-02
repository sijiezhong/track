package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class WebhookSubscriptionIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void createSubscription_sendSignedDelivery_and_replay() throws Exception {
        // 1) 创建订阅
        String secret = "s3cr3t";
        String targetUrl = "http://example.com/hook1";
        String createBody = "{\n" +
                "  \"url\": \"" + targetUrl + "\",\n" +
                "  \"secret\": \"" + secret + "\",\n" +
                "  \"enabled\": true\n" +
                "}";
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        // 2) 触发事件，期望签名头
        String eventPayload = "{\"eventId\":1,\"eventName\":\"wh_evt\",\"tenantId\":1}";
        String signature = hmacSha256Base64(secret, eventPayload);

        server.expect(once(), requestTo(targetUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Webhook-Signature", signature))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        String collectBody = "{\"eventName\":\"wh_evt\",\"sessionId\":\"sess-sub-1\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectBody))
                .andExpect(status().isCreated());

        server.verify();

        // 3) 重放最近一次事件（模拟），要求再次发送并带签名
        String replaySignature = hmacSha256Base64(secret, eventPayload);
        server = MockRestServiceServer.createServer(restTemplate);
        server.expect(once(), requestTo(targetUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Webhook-Signature", replaySignature))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        mockMvc.perform(post("/api/v1/webhooks/replay/latest")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ADMIN"))
                .andExpect(status().isOk());

        server.verify();
    }

    private String hmacSha256Base64(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }
}


