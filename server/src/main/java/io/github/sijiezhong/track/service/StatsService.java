package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.dto.EventStatsResponse;
import io.github.sijiezhong.track.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 统计服务
 * 
 * <p>提供事件统计汇总功能
 * 
 * @author sijie
 */
@Service
public class StatsService {
    
    private static final Logger log = LoggerFactory.getLogger(StatsService.class);
    
    private final EventRepository eventRepository;
    
    public StatsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    /**
     * 获取事件统计数据
     * 
     * @param appId 应用ID
     * @return 统计数据
     */
    public EventStatsResponse getEventStats(Integer appId) {
        log.debug("获取事件统计数据: appId={}", appId);
        
        EventStatsResponse stats = new EventStatsResponse();
        
        // 总事件数
        Long totalEvents = eventRepository.countByTenantId(appId);
        stats.setTotalEvents(totalEvents);
        
        // 今日事件数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
        Long todayEvents = eventRepository.countByTenantIdAndTimeRange(appId, todayStart, todayEnd);
        stats.setTodayEvents(todayEvents);
        
        // 昨日事件数
        LocalDateTime yesterdayStart = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIN);
        LocalDateTime yesterdayEnd = todayStart;
        Long yesterdayEvents = eventRepository.countByTenantIdAndTimeRange(appId, yesterdayStart, yesterdayEnd);
        stats.setYesterdayEvents(yesterdayEvents);
        
        // 计算今日相比昨日的变化百分比
        if (yesterdayEvents > 0) {
            double percent = ((todayEvents - yesterdayEvents) * 100.0) / yesterdayEvents;
            stats.setTodayVsYesterdayPercent(Math.round(percent * 10.0) / 10.0); // 保留1位小数
        } else {
            stats.setTodayVsYesterdayPercent(0.0);
        }
        
        // 独立用户数
        Long uniqueUsers = eventRepository.countDistinctUsersByTenantId(appId);
        stats.setUniqueUsers(uniqueUsers);
        
        // 在线用户数（最近1分钟活跃）
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        Long onlineUsers = eventRepository.countOnlineUsers(appId, oneMinuteAgo);
        stats.setOnlineUsers(onlineUsers);
        
        // 页面访问数（pageview事件）
        Long pageViews = eventRepository.countByTenantIdAndEventName(appId, "pageview");
        stats.setPageViews(pageViews);
        
        // 点击数（click事件）
        Long clicks = eventRepository.countByTenantIdAndEventName(appId, "click");
        stats.setClicks(clicks);
        
        log.debug("统计数据获取完成: totalEvents={}, todayEvents={}, uniqueUsers={}, onlineUsers={}", 
            totalEvents, todayEvents, uniqueUsers, onlineUsers);
        
        return stats;
    }
}

