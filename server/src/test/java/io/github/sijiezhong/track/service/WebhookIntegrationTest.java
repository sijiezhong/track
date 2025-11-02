package io.github.sijiezhong.track.service;

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

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class WebhookIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    @Autowired
    private WebhookSettings webhookSettings;

    @BeforeEach
    void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldInvokeWebhookOnEventCreate() throws Exception {
        String url = "http://example.com/webhook";
        webhookSettings.setEnabled(true);
        webhookSettings.setUrl(url);

        server.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        String body = "{\"eventName\":\"wh_test\",\"sessionId\":\"sess-wh-1\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        server.verify();
    }
}
