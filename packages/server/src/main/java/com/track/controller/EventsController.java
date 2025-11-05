package com.track.controller;

import com.track.dto.EventsListResponse;
import com.track.dto.ProjectsResponse;
import com.track.entity.Event;
import com.track.entity.Project;
import com.track.repository.EventRepository;
import com.track.repository.ProjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件和项目管理 Controller
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Events & Projects", description = "事件列表和应用列表接口")
public class EventsController {
    
    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    
    @Autowired
    public EventsController(EventRepository eventRepository, ProjectRepository projectRepository) {
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
    }
    
    @GetMapping("/events")
    @Operation(summary = "事件列表", description = "查询事件记录（用于用户行为路径等功能）")
    public ResponseEntity<EventsListResponse> getEvents(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        // 构建查询条件
        Specification<Event> spec = Specification.where(null);
        
        if (appId != null && !appId.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("appId"), appId));
        }
        
        if (start != null && end != null) {
            spec = spec.and((root, query, cb) -> 
                cb.between(root.get("serverTimestamp"), start, end));
        }
        
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventTypeId"), type.shortValue()));
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            String keywordPattern = "%" + keyword + "%";
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(root.get("pageUrl"), keywordPattern),
                    cb.like(root.get("pageTitle"), keywordPattern),
                    cb.like(root.get("customEventId"), keywordPattern)
                )
            );
        }
        
        // 分页查询
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "serverTimestamp"));
        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        
        // 转换为 Map（便于 JSON 序列化）
        List<Map<String, Object>> items = eventPage.getContent().stream()
            .map(this::eventToMap)
            .collect(Collectors.toList());
        
        EventsListResponse.PageInfo pageInfo = new EventsListResponse.PageInfo(
            page, size, eventPage.getTotalElements());
        
        EventsListResponse response = new EventsListResponse(items, pageInfo);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/projects")
    @Operation(summary = "应用列表", description = "获取可选的应用列表供筛选")
    public ResponseEntity<ProjectsResponse> getProjects(
            @RequestParam(required = false) Boolean active) {
        
        List<Project> projects;
        if (active != null) {
            projects = projectRepository.findAll().stream()
                .filter(p -> active.equals(p.getIsActive()))
                .collect(Collectors.toList());
        } else {
            projects = projectRepository.findAll();
        }
        
        List<ProjectsResponse.ProjectInfo> projectInfos = projects.stream()
            .map(p -> new ProjectsResponse.ProjectInfo(p.getAppId(), p.getAppName()))
            .collect(Collectors.toList());
        
        ProjectsResponse response = new ProjectsResponse(projectInfos);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 将 Event 实体转换为 Map（便于 JSON 序列化）
     */
    private Map<String, Object> eventToMap(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("appId", event.getAppId());
        map.put("userId", event.getUserId());
        map.put("userProperties", event.getUserProperties());
        map.put("eventTypeId", event.getEventTypeId());
        map.put("eventType", event.getEventType().getName());
        map.put("customEventId", event.getCustomEventId());
        map.put("properties", event.getProperties());
        map.put("domPath", event.getDomPath());
        map.put("pageUrl", event.getPageUrl());
        map.put("pageTitle", event.getPageTitle());
        map.put("referrer", event.getReferrer());
        map.put("userAgent", event.getUserAgent());
        map.put("ipAddress", event.getIpAddress() != null ? event.getIpAddress().getHostAddress() : null);
        map.put("serverTimestamp", event.getServerTimestamp());
        map.put("createdAt", event.getCreatedAt());
        return map;
    }
}

