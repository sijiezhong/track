package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.dto.EventStatsResponse;
import io.github.sijiezhong.track.service.StatsService;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制器
 * 
 * <p>提供事件统计汇总接口
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/events")
@Tag(name = "Event Statistics", description = "事件统计汇总接口")
public class StatsController {
    
    private static final Logger log = LoggerFactory.getLogger(StatsController.class);
    
    private final StatsService statsService;
    
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }
    
    /**
     * 获取事件统计数据
     * 
     * @param appId 应用ID请求头（必填）
     * @return 统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取事件统计数据", 
               description = "返回总事件数、今日事件数、独立用户数、在线用户数等统计信息")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<EventStatsResponse> getStats(
            @Parameter(description = "应用头，必填") 
            @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId) {
        
        log.debug("收到统计数据请求: appId={}", appId);
        
        EventStatsResponse stats = statsService.getEventStats(appId);
        
        return ResponseUtil.success(stats);
    }
}

