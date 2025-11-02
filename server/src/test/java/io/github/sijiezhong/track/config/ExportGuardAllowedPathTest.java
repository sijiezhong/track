package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ExportGuardAllowedPathTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"READONLY"})
    void exportCsvWithTenantHeaderShouldBeOk() throws Exception {
        mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());
    }
}


