package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Contract tests to verify OpenAPI documentation consistency.
 * 
 * These tests verify that:
 * - OpenAPI documentation endpoint is accessible
 * - API documentation contains expected paths and operations
 * - Response schemas are consistent
 * 
 * Note: This is a basic validation. For comprehensive contract testing,
 * consider using Spring Cloud Contract or dedicated contract testing tools.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ApiContractTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI documentation endpoint should be accessible")
    void should_ProvideOpenApiDocumentation_AtExpectedEndpoint() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        
        // Verify OpenAPI structure
        assertThat(json).contains("\"openapi\"");
        assertThat(json).contains("\"info\"");
        assertThat(json).contains("\"paths\"");
    }

    @Test
    @DisplayName("OpenAPI documentation should contain event collection endpoint")
    void should_ContainEventCollectionEndpoint_InOpenApiDocs() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        
        // Verify key endpoints are documented
        assertThat(json).contains("/api/v1/events/collect");
        assertThat(json).contains("/api/v1/events/export.csv");
    }

    @Test
    @DisplayName("Swagger UI endpoint should be accessible")
    void should_ProvideSwaggerUi_AtExpectedEndpoint() throws Exception {
        // Swagger UI is typically served at /swagger-ui.html
        // This test verifies the endpoint is accessible (may redirect or serve HTML)
        var result = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        // Swagger UI may return 200 (HTML) or 302 (redirect)
        assertThat(statusCode).isIn(200, 302);
    }

    @Test
    @DisplayName("OpenAPI documentation should contain analytics endpoints")
    void should_ContainAnalyticsEndpoints_InOpenApiDocs() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        
        // Verify analytics endpoints are documented
        assertThat(json).contains("/api/v1/events/trend");
        assertThat(json).contains("/api/v1/events/funnel");
        assertThat(json).contains("/api/v1/events/segmentation");
    }

    @Test
    @DisplayName("OpenAPI documentation should contain admin endpoints")
    void should_ContainAdminEndpoints_InOpenApiDocs() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        
        // Verify admin endpoints are documented
        assertThat(json).contains("/api/v1/admin");
    }

    @Test
    @DisplayName("OpenAPI documentation should contain webhook endpoints")
    void should_ContainWebhookEndpoints_InOpenApiDocs() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        
        // Verify webhook endpoints are documented
        assertThat(json).contains("/api/v1/webhooks");
    }
}

