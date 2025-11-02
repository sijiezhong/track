package io.github.sijiezhong.track.security;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("secure")
public class SecurityHttpsHstsIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void httpRequestShouldReturnOkWithoutRedirect() throws Exception {
        // 测试环境下允许 http，确保不发生 3xx 跳转
        mockMvc.perform(get("/actuator/health").secure(false))
                .andExpect(status().isOk());
    }

    @Test
    void httpsRequestShouldIncludeHstsHeader() throws Exception {
        // secure 请求应包含 HSTS 响应头
        mockMvc.perform(get("/actuator/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", org.hamcrest.Matchers.containsString("max-age")));
    }
}


