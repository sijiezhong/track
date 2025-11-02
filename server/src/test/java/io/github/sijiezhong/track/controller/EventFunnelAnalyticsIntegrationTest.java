package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.testsupport.EventTestBuilder;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventFunnelAnalyticsIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void setupData() {
        // PostgresTestBase.cleanupDatabase() is already called before this method
        // Additional cleanup here is redundant but kept for clarity
        
        // Prepare two sessions under tenant 1, one completes full funnel: pv -> add_to_cart -> purchase
        Session s1 = SessionTestBuilder.create()
                .withSessionId("sess-funnel-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        s1 = sessionRepository.save(s1);
        
        Session s2 = SessionTestBuilder.create()
                .withSessionId("sess-funnel-2")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        s2 = sessionRepository.save(s2);
        
        Session s3 = SessionTestBuilder.create()
                .withSessionId("sess-funnel-3")
                .withTenantId(SECOND_TENANT_ID) // Noise tenant
                .build();
        s3 = sessionRepository.save(s3);

        LocalDateTime now = LocalDateTime.now().withNano(0);

        // Session 1: Complete path
        eventRepository.save(buildEvent(EVENT_PAGE_VIEW, DEFAULT_TENANT_ID, s1.getId(), now.minusMinutes(30)));
        eventRepository.save(buildEvent(EVENT_ADD_TO_CART, DEFAULT_TENANT_ID, s1.getId(), now.minusMinutes(20)));
        eventRepository.save(buildEvent(EVENT_PURCHASE, DEFAULT_TENANT_ID, s1.getId(), now.minusMinutes(10)));

        // Session 2: Only reaches step 2
        eventRepository.save(buildEvent(EVENT_PAGE_VIEW, DEFAULT_TENANT_ID, s2.getId(), now.minusMinutes(25)));
        eventRepository.save(buildEvent(EVENT_ADD_TO_CART, DEFAULT_TENANT_ID, s2.getId(), now.minusMinutes(15)));

        // Another tenant complete path (should not be counted)
        eventRepository.save(buildEvent(EVENT_PAGE_VIEW, SECOND_TENANT_ID, s3.getId(), now.minusMinutes(30)));
        eventRepository.save(buildEvent(EVENT_ADD_TO_CART, SECOND_TENANT_ID, s3.getId(), now.minusMinutes(20)));
        eventRepository.save(buildEvent(EVENT_PURCHASE, SECOND_TENANT_ID, s3.getId(), now.minusMinutes(10)));
    }

    private Event buildEvent(String name, int tenantId, Long sessionPk, LocalDateTime time) {
        return EventTestBuilder.create()
                .withEventName(name)
                .withTenantId(tenantId)
                .withSessionId(sessionPk)
                .withEventTime(time)
                .withProperties(DEFAULT_PROPERTIES)
                .build();
    }

    @Test
    void funnelShouldReturnPerStepCountsAndConversion() throws Exception {
        String steps = "page_view,add_to_cart,purchase";
        MvcResult res = mockMvc.perform(get("/api/v1/events/funnel")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("steps", steps)
                        .param("windowDays", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        // 期望返回结构类似：
        // {
        //   "steps":["page_view","add_to_cart","purchase"],
        //   "counts":[2,2,1],
        //   "conversionRates":[1.0, 1.0, 0.5]
        // }
        Assertions.assertTrue(json.contains("\"steps\""));
        Assertions.assertTrue(json.contains("page_view") && json.contains("add_to_cart") && json.contains("purchase"));
        Assertions.assertTrue(json.contains("\"counts\""));
        // 核心断言：第一步2个、第二步2个、第三步1个（仅租户1数据）
        Assertions.assertTrue(json.contains("[2,2,1]") || json.contains("\"counts\":[2,2,1]"));
        Assertions.assertTrue(json.contains("\"conversionRates\""));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P0: 租户1的漏斗分析不应包含租户2的数据")
    void should_NotIncludeOtherTenantData_InFunnelAnalysis() throws Exception {
        // Given: setupData()已经创建了租户1的漏斗数据（2个pv, 2个add_to_cart, 1个purchase）
        // 和租户2的完整漏斗数据（1个pv, 1个add_to_cart, 1个purchase）
        
        // When: 租户1查询漏斗分析
        String steps = "page_view,add_to_cart,purchase";
        MvcResult res = mockMvc.perform(get("/api/v1/events/funnel")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("steps", steps)
                        .param("windowDays", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        
        // Then: 验证counts应该是[2,2,1]（租户1的数据），不应该包含租户2的数据
        Assertions.assertTrue(json.contains("[2,2,1]") || json.contains("\"counts\":[2,2,1]"),
                "Tenant 1 should have counts [2,2,1], not including tenant 2 data");
        
        // When: 租户2查询漏斗分析
        MvcResult res2 = mockMvc.perform(get("/api/v1/events/funnel")
                        .header("X-Tenant-Id", 2)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("steps", steps)
                        .param("windowDays", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String json2 = res2.getResponse().getContentAsString();
        
        // Then: 验证租户2的counts应该是[1,1,1]（租户2自己的数据）
        Assertions.assertTrue(json2.contains("[1,1,1]") || json2.contains("\"counts\":[1,1,1]"),
                "Tenant 2 should have counts [1,1,1], not including tenant 1 data");
        
        // 验证两个租户的结果不同
        Assertions.assertNotEquals(json, json2, "Tenant 1 and Tenant 2 should have different funnel results");
    }
}


