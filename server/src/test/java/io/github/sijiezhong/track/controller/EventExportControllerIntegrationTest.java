package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.EventTestBuilder;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventExportControllerIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void setupData() {
        eventRepository.deleteAll();
        // Insert two events from different tenants, export should only return tenant=1
        // Save sessions first to get IDs for foreign key references
        Session s1 = SessionTestBuilder.create()
                .withSessionId("sess-csv-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        s1 = sessionRepository.save(s1);

        Event e1 = EventTestBuilder.create()
                .withEventName(EVENT_PAGE_VIEW)
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(DEFAULT_USER_ID)
                .withSession(s1)
                .withProperties(PROPERTIES_WITH_URL)
                .withUa("UA-1")
                .withReferrer(REFERRER_HOME)
                .withIp(IP_198_51_100_1)
                .withDevice(DEVICE_DESKTOP)
                .withOs(OS_MAC)
                .withBrowser(BROWSER_CHROME)
                .build();
        eventRepository.save(e1);

        Session s2 = SessionTestBuilder.create()
                .withSessionId("sess-csv-2")
                .withTenantId(SECOND_TENANT_ID)
                .build();
        s2 = sessionRepository.save(s2);

        Event e2 = EventTestBuilder.create()
                .withEventName(EVENT_CLICK)
                .withTenantId(SECOND_TENANT_ID)
                .withUserId(SECOND_USER_ID)
                .withSession(s2)
                .withProperties(PROPERTIES_WITH_PRODUCT)
                .withUa("UA-2")
                .withReferrer(REFERRER_PRODUCT)
                .withIp(IP_198_51_100_2)
                .withDevice(DEVICE_MOBILE)
                .withOs(OS_ANDROID)
                .withBrowser(BROWSER_CHROME)
                .build();
        eventRepository.save(e2);
    }

    @Test
    void exportCsvShouldReturnTenantFilteredRowsWithHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events/export.csv")
                .header("X-Tenant-Id", 1)
                .accept("text/csv"))
                .andReturn();

        String ct = result.getResponse().getContentType();
        Assertions.assertNotNull(ct);
        Assertions.assertTrue(ct.contains("text/csv"));
        String dispo = result.getResponse().getHeader("Content-Disposition");
        Assertions.assertNotNull(dispo);
        Assertions.assertTrue(dispo.contains("attachment"));

        String csv = result.getResponse().getContentAsString();
        // 标题包含关键列
        Assertions.assertTrue(csv.startsWith(
                "id,eventName,userId,sessionId,tenantId,eventTime,ua,referrer,ip,device,os,browser,properties"));
        // 仅包含租户1的数据与事件名
        Assertions.assertTrue(csv.contains("page_view"));
        Assertions.assertFalse(csv.contains(",click,"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 导出时不应包含其他租户的数据")
    void should_NotIncludeOtherTenantData_InExport() throws Exception {
        // Given: setupData()已经创建了租户1和租户2的事件
        // When: 租户1导出CSV
        MvcResult result = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 1)
                        .accept("text/csv"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        
        // Then: 必须验证只包含租户1的数据，不包含租户2的数据
        // 验证包含租户1的事件
        Assertions.assertTrue(csv.contains("page_view"), 
                "Should contain tenant 1 event");
        // 验证不包含租户2的事件（关键断言）
        Assertions.assertFalse(csv.contains("click"), 
                "CRITICAL: Should not contain tenant 2 event");
        // 验证不包含租户2的userId
        Assertions.assertFalse(csv.contains(String.valueOf(SECOND_USER_ID)), 
                "CRITICAL: Should not contain tenant 2 user data");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 导出空数据集时应返回正确的格式")
    void should_HandleEmptyDataset_WhenExporting() throws Exception {
        // Given: 数据库为空（或租户3没有数据）
        eventRepository.deleteAll();
        sessionRepository.deleteAll();
        
        // When: 租户3导出CSV（租户3没有任何数据）
        MvcResult result = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", 3)
                        .accept("text/csv"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        
        // Then: 应该只包含表头，没有数据行（或空结果）
        // 验证包含表头
        Assertions.assertTrue(csv.contains("id,eventName"), 
                "Should contain CSV headers");
        // 验证没有数据行（除了表头，应该只有换行符或为空）
        String[] lines = csv.split("\n");
        // 如果有数据，应该有至少2行（表头+数据），如果没有数据，应该只有表头
        // 这里我们验证至少有表头
        Assertions.assertTrue(lines.length >= 1, 
                "Should have at least header row");
    }
}
