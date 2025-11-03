package io.github.sijiezhong.track.dto;

/**
 * 事件统计响应DTO
 * 
 * @author sijie
 */
public class EventStatsResponse {
    
    private Long totalEvents;           // 总事件数
    private Long todayEvents;           // 今日事件数
    private Long yesterdayEvents;       // 昨日事件数
    private Long uniqueUsers;           // 独立用户数
    private Long onlineUsers;           // 在线用户数（最近1分钟活跃）
    private Long pageViews;             // 页面访问数（pageview事件）
    private Long clicks;                // 点击数（click事件）
    private Double todayVsYesterdayPercent; // 今日相比昨日的变化百分比
    
    public EventStatsResponse() {}
    
    public Long getTotalEvents() {
        return totalEvents;
    }
    
    public void setTotalEvents(Long totalEvents) {
        this.totalEvents = totalEvents;
    }
    
    public Long getTodayEvents() {
        return todayEvents;
    }
    
    public void setTodayEvents(Long todayEvents) {
        this.todayEvents = todayEvents;
    }
    
    public Long getYesterdayEvents() {
        return yesterdayEvents;
    }
    
    public void setYesterdayEvents(Long yesterdayEvents) {
        this.yesterdayEvents = yesterdayEvents;
    }
    
    public Long getUniqueUsers() {
        return uniqueUsers;
    }
    
    public void setUniqueUsers(Long uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }
    
    public Long getOnlineUsers() {
        return onlineUsers;
    }
    
    public void setOnlineUsers(Long onlineUsers) {
        this.onlineUsers = onlineUsers;
    }
    
    public Long getPageViews() {
        return pageViews;
    }
    
    public void setPageViews(Long pageViews) {
        this.pageViews = pageViews;
    }
    
    public Long getClicks() {
        return clicks;
    }
    
    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }
    
    public Double getTodayVsYesterdayPercent() {
        return todayVsYesterdayPercent;
    }
    
    public void setTodayVsYesterdayPercent(Double todayVsYesterdayPercent) {
        this.todayVsYesterdayPercent = todayVsYesterdayPercent;
    }
}

