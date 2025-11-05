package com.track.service;

import com.track.dto.SessionInfo;
import com.track.entity.Event;
import com.track.entity.EventType;
import com.track.entity.Project;
import com.track.dto.TrackBatchRequest;
import com.track.repository.EventRepository;
import com.track.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件处理服务
 */
@Service
public class TrackService {
    
    private static final Logger log = LoggerFactory.getLogger(TrackService.class);
    
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    
    public TrackService(ProjectRepository projectRepository, EventRepository eventRepository) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
    }
    
    /**
     * 验证 AppId 是否存在且激活
     * @param appId 应用 ID
     * @return true 如果有效，false 如果无效
     */
    public boolean validateAppId(String appId) {
        return projectRepository.findByAppId(appId)
            .map(Project::getIsActive)
            .orElse(false);
    }
    
    /**
     * 异步处理批量事件
     * @param request 批量事件请求
     */
    @Async
    @Transactional
    public void processBatchEventsAsync(TrackBatchRequest request) {
        // 验证AppId（从 Session 中获取的 appId）
        Project project = projectRepository.findByAppId(request.getAppId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid appId: " + request.getAppId()));
        
        if (!project.getIsActive()) {
            log.warn("Attempt to track inactive project: {}", request.getAppId());
            return;
        }
        
        // 处理事件
        processBatchEvents(request);
    }
    
    /**
     * 处理批量事件
     * @param request 批量事件请求
     */
    private void processBatchEvents(TrackBatchRequest request) {
        List<Event> events = new ArrayList<>();
        
        for (TrackBatchRequest.EventDTO eventDTO : request.getE()) {
            // 将客户端传来的数字类型转换为枚举
            EventType eventType = EventType.fromCode(eventDTO.getT());
            
            Event event = new Event();
            event.setAppId(request.getAppId());
            event.setUserId(request.getUserId());
            event.setUserProperties(request.getUserProps());
            event.setEventTypeId((short) eventType.getCode()); // 存储枚举的 code 值
            event.setCustomEventId(eventDTO.getId());
            event.setProperties(eventDTO.getP());
            
            // 如果是点击事件，提取 DOM 路径
            if (eventType == EventType.CLICK && eventDTO.getP() != null) {
                event.setDomPath((String) eventDTO.getP().get("domPath"));
            }
            
            // 从 properties 中提取页面信息（如果存在）
            if (eventDTO.getP() != null) {
                event.setPageUrl((String) eventDTO.getP().get("pageUrl"));
                event.setPageTitle((String) eventDTO.getP().get("pageTitle"));
                event.setReferrer((String) eventDTO.getP().get("referrer"));
            }
            
            event.setUserAgent(request.getUserAgent());
            
            // 解析 IP 地址
            if (request.getIpAddress() != null) {
                try {
                    event.setIpAddress(InetAddress.getByName(request.getIpAddress()));
                } catch (Exception e) {
                    log.warn("Failed to parse IP address: {}", request.getIpAddress(), e);
                }
            }
            
            event.setServerTimestamp(request.getServerTimestamp() != null ? 
                request.getServerTimestamp() : LocalDateTime.now());
            
            events.add(event);
        }
        
        eventRepository.saveAll(events);
    }
}

