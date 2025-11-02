package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
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
public class EventExportEdgesIntegrationTest extends PostgresTestBase {

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
        s.setSessionId("sess-exp-edge");
        s.setTenantId(1);
        s = sessionRepository.save(s);
        Event e = new Event();
        e.setEventName("pv");
        e.setTenantId(1);
        e.setSessionId(s.getId());
        e.setEventTime(LocalDateTime.now());
        e.setProperties("{\"k\":\"v\"}");
        eventRepository.save(e);
    }

    @Test
    void csvMissingTenantHeaderShould403() throws Exception {
        mockMvc.perform(get("/api/v1/events/export.csv").accept("text/csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"READONLY"})
    void csvReadonlyRoleShouldHidePropertiesColumn() throws Exception {
        String csv = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String header = csv.split("\n", 2)[0];
        assertThat(header).doesNotContain("properties");
        assertThat(header).contains("browser");
    }

    @Test
    void parquetExportShouldReturnBytesWithMagic() throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/events/export.parquet")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        // Parquet file must have at least 4 bytes for magic number "PAR1"
        assertThat(body).hasSizeGreaterThanOrEqualTo(4);
        // Verify Parquet magic number
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'A');
        assertThat(body[2]).isEqualTo((byte) 'R');
        assertThat(body[3]).isEqualTo((byte) '1');
    }
}


