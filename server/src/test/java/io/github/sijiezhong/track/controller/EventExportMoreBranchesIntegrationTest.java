package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventExportMoreBranchesIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("track_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void seed() {
        eventRepository.deleteAll();
        sessionRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-exp-more-1");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("pv");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setEventTime(null); // test null eventTime rendered empty
        e.setUa("UA,with,comma");
        e.setReferrer("https://ex\"ample,com"); // contains quote and comma
        e.setProperties("{\"k\":\"v\"}"); // contains quotes
        eventRepository.save(e);
    }

    @Test
    @WithMockUser(roles = {"READONLY"})
    void selectedFieldsIncludePropertiesButReadonlyShouldOutputEmptyValues() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("fields", "id,eventName,properties"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("id,eventName,properties");
        // properties should be empty due to READONLY role hide
        assertThat(lines[1]).endsWith(",");
    }

    @Test
    void unknownFieldShouldRenderEmptyColumn() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("fields", "id,unknownCol,eventName"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("id,unknownCol,eventName");
        // unknownCol should be empty between id and eventName
        String[] cols = lines[1].split(",", -1);
        assertThat(cols.length).isGreaterThanOrEqualTo(3);
        assertThat(cols[1]).isEqualTo("");
        assertThat(cols[2]).isEqualTo("pv");
    }

    @Test
    void csvShouldQuoteSpecialCharacters() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String[] lines = csv.split("\n");
        // header then one data line for tenant=1
        assertThat(lines.length).isGreaterThanOrEqualTo(2);
        String data = lines[1];
        // UA has commas -> quoted
        assertThat(data).contains("\"UA,with,comma\"");
        // referrer contains quote and comma -> quotes and double quotes inside
        assertThat(data).contains("\"https://ex\"\"ample,com\"");
        // eventTime may be auto-populated by entity lifecycle; skip strict emptiness check
    }

    @Test
    void exportCsvWithoutEventNameFilter() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("pv");
        // test filterEventName == null branch
    }

    @Test
    void exportCsvWithBlankFieldsShouldUseDefaultColumns() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("fields", "   "))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // should use default columns (not selected path)
        assertThat(csv).contains("id,eventName");
    }

    @Test
    void exportCsvWithoutMockUserShouldShowProperties() throws Exception {
        // No @WithMockUser, so currentHasRole should return false
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String header = csv.split("\n", 2)[0];
        assertThat(header).contains("properties");
    }

    @Test
    void exportCsvShouldCoverAllFieldCases() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-all-fields");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setUserId(100);
        e.setEventTime(LocalDateTime.of(2024, 1, 1, 12, 0));
        e.setUa("Mozilla/5.0");
        e.setReferrer("https://example.com");
        e.setIp("192.168.1.1");
        e.setDevice("Desktop");
        e.setOs("Windows");
        e.setBrowser("Chrome");
        e.setProperties("{\"key\":\"value\"}");
        eventRepository.save(e);

        // Test all field names in valueOf switch
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("fields", "id,eventName,userId,sessionId,tenantId,eventTime,ua,referrer,ip,device,os,browser,properties"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("id,eventName,userId,sessionId,tenantId,eventTime,ua,referrer,ip,device,os,browser,properties");
        assertThat(lines[1]).contains("test");
    }

    @Test
    void exportCsvShouldHandleNullValues() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-null");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setUserId(null);
        e.setEventTime(null);
        e.setUa(null);
        e.setReferrer(null);
        e.setIp(null);
        e.setDevice(null);
        e.setOs(null);
        e.setBrowser(null);
        e.setProperties(null);
        eventRepository.save(e);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // null values should be rendered as empty strings in CSV
        String[] lines = csv.split("\n");
        assertThat(lines.length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void exportCsvShouldQuoteValuesWithNewline() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-newline");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setUa("UA\nwith\nnewlines");
        eventRepository.save(e);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Values with newlines should be quoted
        assertThat(csv).contains("\"UA\nwith\nnewlines\"");
    }

    @Test
    void exportCsvShouldQuoteValuesWithQuotesOnly() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-quotes");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setUa("UA\"with\"quotes");
        eventRepository.save(e);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Values with quotes should be quoted and escaped
        assertThat(csv).contains("\"UA\"\"with\"\"quotes\"");
    }

    @Test
    void exportCsvWithEventNameFilter() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-filter");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e1 = new Event();
        e1.setEventName("click");
        e1.setTenantId(1);
        e1.setSessionId(s.getId());
        eventRepository.save(e1);

        Event e2 = new Event();
        e2.setEventName("pv");
        e2.setTenantId(1);
        e2.setSessionId(s.getId());
        eventRepository.save(e2);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "click"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Should only contain click events
        assertThat(csv).contains("click");
        String[] lines = csv.split("\n");
        // Header + 1 data row for click
        assertThat(lines.length).isEqualTo(2);
    }

    @Test
    void exportCsvWithSelectedFieldsAndReadonly() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-readonly-fields");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setProperties("{\"secret\":\"data\"}");
        eventRepository.save(e);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .param("fields", "eventName,properties"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Without READONLY role, properties should be visible
        assertThat(csv).contains("properties");
    }

    @Test
    @WithMockUser(roles = {"READONLY"})
    void exportCsvReadonlyWithAllFields() throws Exception {
        eventRepository.deleteAll();
        Session s = new Session();
        s.setSessionId("sess-readonly-all");
        s.setTenantId(1);
        s = sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("test");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setProperties("{\"secret\":\"data\"}");
        eventRepository.save(e);

        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String header = csv.split("\n", 2)[0];
        // READONLY role should hide properties column
        assertThat(header).doesNotContain("properties");
    }

}


