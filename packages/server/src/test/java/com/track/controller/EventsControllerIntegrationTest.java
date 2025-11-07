package com.track.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.track.dto.EventsListResponse;
import com.track.dto.ProjectsResponse;
import com.track.entity.Event;
import com.track.entity.EventType;
import com.track.entity.Project;
import com.track.repository.EventRepository;
import com.track.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EventsController 集成测试
 * 验证事件列表和应用列表接口的正确性，包括分页、筛选、排序逻辑
 */
@WebMvcTest(controllers = EventsController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EventsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private ProjectRepository projectRepository;

    private String appId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        appId = "test-app";
        startTime = LocalDateTime.now().minusDays(7);
        endTime = LocalDateTime.now();
    }

    // ========== getEvents 测试 ==========

    @Test
    void testGetEvents_Success() throws Exception {
        // Given - 创建测试事件数据
        List<Event> events = createTestEvents(3);
        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 50), 3L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then
        String response = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.page.index").value(1))
            .andExpect(jsonPath("$.page.size").value(50))
            .andExpect(jsonPath("$.page.total").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result = objectMapper.readValue(response, EventsListResponse.class);
        assertEquals(3, result.getItems().size());
        assertEquals(1, result.getPage().getIndex());
        assertEquals(50, result.getPage().getSize());
        assertEquals(3L, result.getPage().getTotal());
        
        // 验证事件数据包含必要字段
        Map<String, Object> firstEvent = result.getItems().get(0);
        assertTrue(firstEvent.containsKey("id"));
        assertTrue(firstEvent.containsKey("appId"));
        assertTrue(firstEvent.containsKey("eventType"));
        assertTrue(firstEvent.containsKey("serverTimestamp"));
    }

    @Test
    void testGetEvents_WithEmptyStringDates() throws Exception {
        // Given - 测试空字符串日期参数（前端可能传递空字符串）
        List<Event> events = createTestEvents(2);
        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 50), 2L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then - 传递空字符串应该被正确处理，不抛出异常
        String response = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("start", "")  // 空字符串
                .param("end", "")     // 空字符串
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk())  // 应该正常返回，而不是 500 错误
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result = objectMapper.readValue(response, EventsListResponse.class);
        assertEquals(2, result.getItems().size());
        // 验证空字符串参数被正确处理，不会导致日期范围筛选
    }

    @Test
    void testGetEvents_WithoutAppId_ShouldReturn400() throws Exception {
        // Given - appId 现在是必填的，不传应该返回 400 错误

        // When & Then - 不传 appId 应该返回 400 Bad Request
        mockMvc.perform(get("/api/events")
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetEvents_WithEmptyAppId_ShouldReturn400() throws Exception {
        // Given - appId 为空字符串应该返回 400 错误
        
        // When & Then - 传递空字符串 appId 应该返回 400 Bad Request
        mockMvc.perform(get("/api/events")
                .param("appId", "")
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetEvents_Pagination() throws Exception {
        // Given - 测试分页逻辑（分别测试第一页和第二页）
        List<Event> page1Events = createTestEvents(50);
        Page<Event> page1 = new PageImpl<>(page1Events, PageRequest.of(0, 50), 80L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page1);

        // When & Then - 第一页
        String response1 = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.index").value(1))
            .andExpect(jsonPath("$.page.size").value(50))
            .andExpect(jsonPath("$.page.total").value(80))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result1 = objectMapper.readValue(response1, EventsListResponse.class);
        assertEquals(50, result1.getItems().size());
        assertEquals(80L, result1.getPage().getTotal());
    }

    @Test
    void testGetEvents_PaginationSecondPage() throws Exception {
        // Given - 测试第二页
        List<Event> page2Events = createTestEvents(30);
        Page<Event> page2 = new PageImpl<>(page2Events, PageRequest.of(1, 50), 80L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page2);

        // When & Then - 第二页
        String response2 = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("page", "2")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.index").value(2))
            .andExpect(jsonPath("$.page.size").value(50))
            .andExpect(jsonPath("$.page.total").value(80))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result2 = objectMapper.readValue(response2, EventsListResponse.class);
        assertEquals(30, result2.getItems().size());
    }

    @Test
    void testGetEvents_DefaultPagination() throws Exception {
        // Given
        List<Event> events = createTestEvents(10);
        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 50), 10L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then - 不传 page 和 size 应该使用默认值
        mockMvc.perform(get("/api/events")
                .param("appId", appId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.index").value(1))
            .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void testGetEvents_FilterByType() throws Exception {
        // Given - 测试按事件类型筛选
        List<Event> clickEvents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Event event = createTestEvent();
            event.setEventType(EventType.CLICK);
            clickEvents.add(event);
        }
        Page<Event> eventPage = new PageImpl<>(clickEvents, PageRequest.of(0, 50), 5L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then
        String response = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("type", "2")) // CLICK 类型 code 为 2
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(5))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result = objectMapper.readValue(response, EventsListResponse.class);
        assertEquals(5, result.getItems().size());
        // 验证所有事件都是 CLICK 类型
        result.getItems().forEach(item -> {
            assertEquals("click", item.get("eventType"));
        });
    }

    @Test
    void testGetEvents_FilterByKeyword() throws Exception {
        // Given - 测试关键词搜索（pageUrl, pageTitle, customEventId）
        List<Event> events = new ArrayList<>();
        Event event1 = createTestEvent();
        event1.setPageUrl("https://example.com/search");
        event1.setPageTitle("Search Page");
        events.add(event1);
        
        Event event2 = createTestEvent();
        event2.setCustomEventId("search_submit");
        events.add(event2);

        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 50), 2L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then
        String response = mockMvc.perform(get("/api/events")
                .param("appId", appId)
                .param("keyword", "search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result = objectMapper.readValue(response, EventsListResponse.class);
        assertEquals(2, result.getItems().size());
    }

    @Test
    void testGetEvents_SortByTimestamp() throws Exception {
        // Given - 验证按时间降序排序（业务逻辑要求）
        List<Event> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 3; i++) {
            Event event = createTestEvent();
            event.setServerTimestamp(now.minusHours(i));
            events.add(event);
        }
        
        // 按时间降序排序（最新的在前）
        events.sort((a, b) -> b.getServerTimestamp().compareTo(a.getServerTimestamp()));
        
        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 50), 3L);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(eventPage);

        // When & Then
        String response = mockMvc.perform(get("/api/events")
                .param("appId", appId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsListResponse result = objectMapper.readValue(response, EventsListResponse.class);
        assertEquals(3, result.getItems().size());
        
        // 验证时间戳是降序的（最新的在前）
        List<LocalDateTime> timestamps = new ArrayList<>();
        result.getItems().forEach(item -> {
            String timestampStr = (String) item.get("serverTimestamp");
            if (timestampStr != null) {
                timestamps.add(LocalDateTime.parse(timestampStr.replace("Z", "")));
            }
        });
        
        if (timestamps.size() >= 2) {
            assertTrue(timestamps.get(0).isAfter(timestamps.get(1)) || 
                      timestamps.get(0).isEqual(timestamps.get(1)));
        }
    }

    // ========== getProjects 测试 ==========

    @Test
    void testGetProjects_Success() throws Exception {
        // Given
        List<Project> projects = Arrays.asList(
            new Project(1L, "app1", "App 1", null, null, true, null),
            new Project(2L, "app2", "App 2", null, null, true, null),
            new Project(3L, "app3", "App 3", null, null, false, null)
        );

        when(projectRepository.findAll()).thenReturn(projects);

        // When & Then
        String response = mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(3))
            .andExpect(jsonPath("$.list[0].appId").value("app1"))
            .andExpect(jsonPath("$.list[0].appName").value("App 1"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        ProjectsResponse result = objectMapper.readValue(response, ProjectsResponse.class);
        assertEquals(3, result.getList().size());
        assertEquals("app1", result.getList().get(0).getAppId());
        assertEquals("App 1", result.getList().get(0).getAppName());
    }

    @Test
    void testGetProjects_FilterByActive() throws Exception {
        // Given - 测试只返回活跃项目
        List<Project> allProjects = Arrays.asList(
            new Project(1L, "app1", "App 1", null, null, true, null),
            new Project(2L, "app2", "App 2", null, null, true, null),
            new Project(3L, "app3", "App 3", null, null, false, null)
        );

        when(projectRepository.findAll()).thenReturn(allProjects);

        // When & Then
        String response = mockMvc.perform(get("/api/projects")
                .param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ProjectsResponse result = objectMapper.readValue(response, ProjectsResponse.class);
        // 验证所有返回的项目都是活跃的
        result.getList().forEach(project -> {
            assertTrue(allProjects.stream()
                .anyMatch(p -> p.getAppId().equals(project.getAppId()) && p.getIsActive()));
        });
    }

    @Test
    void testGetProjects_FilterByInactive() throws Exception {
        // Given - 测试只返回非活跃项目
        List<Project> allProjects = Arrays.asList(
            new Project(1L, "app1", "App 1", null, null, true, null),
            new Project(2L, "app2", "App 2", null, null, true, null),
            new Project(3L, "app3", "App 3", null, null, false, null)
        );

        when(projectRepository.findAll()).thenReturn(allProjects);

        // When & Then
        String response = mockMvc.perform(get("/api/projects")
                .param("active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ProjectsResponse result = objectMapper.readValue(response, ProjectsResponse.class);
        // 验证所有返回的项目都是非活跃的
        result.getList().forEach(project -> {
            assertTrue(allProjects.stream()
                .anyMatch(p -> p.getAppId().equals(project.getAppId()) && !p.getIsActive()));
        });
    }

    @Test
    void testGetProjects_EmptyList() throws Exception {
        // Given
        when(projectRepository.findAll()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(0));
    }

    // ========== 辅助方法 ==========

    private List<Event> createTestEvents(int count) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createTestEvent());
        }
        return events;
    }

    private Event createTestEvent() {
        Event event = new Event();
        event.setId((long) (Math.random() * 10000));
        event.setAppId(appId);
        event.setUserId("user-" + (int)(Math.random() * 1000));
        event.setUserProperties(new HashMap<>());
        event.setEventType(EventType.PAGE_VIEW);
        event.setCustomEventId(null);
        event.setProperties(new HashMap<>());
        event.setDomPath("/body/div");
        event.setPageUrl("https://example.com/page");
        event.setPageTitle("Test Page");
        event.setReferrer("https://example.com");
        event.setUserAgent("Mozilla/5.0");
        try {
            event.setIpAddress(InetAddress.getByName("127.0.0.1"));
        } catch (Exception e) {
            // 忽略
        }
        event.setServerTimestamp(LocalDateTime.now().minusHours((long)(Math.random() * 168)));
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}

