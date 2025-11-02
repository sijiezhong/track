package io.github.sijiezhong.track.controller;

 
import io.github.sijiezhong.track.repository.ApplicationRepository;
import io.github.sijiezhong.track.repository.UserRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ApplicationUserManagementIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createAndListApplicationsShouldRespectTenantIsolation() throws Exception {
        String appJson = "{\"appKey\":\"k1\",\"appName\":\"App One\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson))
                .andExpect(status().isCreated());

        // mismatch tenant forbidden
        String appJson2 = "{\"appKey\":\"k2\",\"appName\":\"App Two\",\"tenantId\":2}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson2))
                .andExpect(status().isForbidden());

        // list should return only tenant=1 apps
        MvcResult res = mockMvc.perform(get("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        Assertions.assertTrue(json.contains("k1"));
        Assertions.assertFalse(json.contains("k2"));
    }

    @Test
    void createAndListUsersShouldRespectTenantIsolation() throws Exception {
        String u1 = "{\"username\":\"u1\",\"password\":\"p\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(u1))
                .andExpect(status().isCreated());

        String u2 = "{\"username\":\"u2\",\"password\":\"p\",\"tenantId\":2}";
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(u2))
                .andExpect(status().isForbidden());

        MvcResult res = mockMvc.perform(get("/api/v1/admin/users")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        Assertions.assertTrue(json.contains("u1"));
        Assertions.assertFalse(json.contains("u2"));
    }
}


