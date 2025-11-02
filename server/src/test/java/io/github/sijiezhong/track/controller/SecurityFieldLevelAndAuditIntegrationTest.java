package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.AuditLog;
import io.github.sijiezhong.track.repository.AuditLogRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SecurityFieldLevelAndAuditIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void readonlyExportShouldHidePropertiesField() throws Exception {
        var res = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:READONLY"))
                .andExpect(status().isOk())
                .andReturn();
        String csv = res.getResponse().getContentAsString();
        // 断言表头不包含 properties
        Assertions.assertTrue(csv.startsWith("id,eventName,userId,sessionId,tenantId,eventTime,ua,referrer,ip,device,os,browser\n"));
    }

    @Test
    void adminWriteShouldCreateAuditLog() throws Exception {
        auditLogRepository.deleteAll();
        String body = "{\"appKey\":\"app1\",\"appName\":\"app1\"}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        // 断言已经记录审计
        java.util.List<AuditLog> logs = auditLogRepository.findAll();
        Assertions.assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        Assertions.assertEquals("POST", log.getMethod());
        Assertions.assertTrue(log.getPath().contains("/api/v1/admin/apps"));
        Assertions.assertEquals(1, log.getTenantId());
    }
}


