package com.track.service;

import com.track.dto.*;
import com.track.entity.Event;
import com.track.entity.EventType;
import com.track.repository.EventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 * 提供 PV/UV 统计和分析功能
 */
@Service
public class AnalyticsService {

    private final EventRepository eventRepository;
    private final EntityManager entityManager;

    public AnalyticsService(EventRepository eventRepository, EntityManager entityManager) {
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
    }

    /**
     * 统计 PV（页面浏览量）
     */
    public long getPV(String appId, LocalDateTime startTime, LocalDateTime endTime, String pageUrl) {
        try {
            Specification<Event> spec = Specification.where(null);

            if (appId != null && !appId.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("appId"), appId));
            }

            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventTypeId"), EventType.PAGE_VIEW.getCode()));

            // 只有当提供了时间范围时才添加时间条件
            if (startTime != null && endTime != null) {
                spec = spec.and((root, query, cb) -> cb.between(root.get("serverTimestamp"), startTime, endTime));
            }

            if (pageUrl != null && !pageUrl.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("pageUrl"), pageUrl));
            }

            return eventRepository.count(spec);
        } catch (Exception e) {
            // 记录错误并返回默认值，避免接口崩溃
            org.slf4j.LoggerFactory.getLogger(AnalyticsService.class)
                    .error("Error calculating PV", e);
            return 0L;
        }
    }

    /**
     * 统计 UV（独立访客数）
     */
    public long getUV(String appId, LocalDateTime startTime, LocalDateTime endTime, String pageUrl) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<Event> root = query.from(Event.class);

            List<Predicate> predicates = new ArrayList<>();

            if (appId != null && !appId.isEmpty()) {
                predicates.add(cb.equal(root.get("appId"), appId));
            }

            predicates.add(cb.equal(root.get("eventTypeId"), EventType.PAGE_VIEW.getCode()));

            // 只有当提供了时间范围时才添加时间条件
            if (startTime != null && endTime != null) {
                predicates.add(cb.between(root.get("serverTimestamp"), startTime, endTime));
            }

            if (pageUrl != null && !pageUrl.isEmpty()) {
                predicates.add(cb.equal(root.get("pageUrl"), pageUrl));
            }

            query.select(cb.countDistinct(root.get("userId")))
                    .where(predicates.toArray(new Predicate[0]));

            TypedQuery<Long> typedQuery = entityManager.createQuery(query);
            Long result = typedQuery.getSingleResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            // 记录错误并返回默认值，避免接口崩溃
            org.slf4j.LoggerFactory.getLogger(AnalyticsService.class)
                    .error("Error calculating UV", e);
            return 0L;
        }
    }

    /**
     * 计算跳出率
     */
    public double getBounceRate(String appId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            long totalUV = getUV(appId, startTime, endTime, null);

            if (totalUV == 0) {
                return 0.0;
            }

            // 使用原生 SQL 查询只访问了一个页面的用户数
            String timeCondition = (startTime != null && endTime != null)
                    ? "AND e1.server_timestamp BETWEEN :startTime AND :endTime"
                    : "";
            String subTimeCondition = (startTime != null && endTime != null)
                    ? "AND e2.server_timestamp BETWEEN :startTime AND :endTime"
                    : "";

            String sql = """
                    SELECT COUNT(DISTINCT e1.user_id)
                    FROM events e1
                    WHERE (:appId IS NULL OR e1.app_id = :appId)
                      AND e1.event_type_id = :eventTypeId
                      """ + timeCondition + """
                    AND (
                      SELECT COUNT(DISTINCT e2.page_url)
                      FROM events e2
                      WHERE (:appId IS NULL OR e2.app_id = :appId)
                        AND e2.event_type_id = :eventTypeId
                        """ + subTimeCondition + """
                          AND e2.user_id = e1.user_id
                      ) = 1
                    """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("appId", appId);
            query.setParameter("eventTypeId", EventType.PAGE_VIEW.getCode());
            if (startTime != null && endTime != null) {
                query.setParameter("startTime", startTime);
                query.setParameter("endTime", endTime);
            }

            try {
                Object result = query.getSingleResult();
                Long bounceUV = result != null ? ((Number) result).longValue() : 0L;
                return (double) bounceUV / totalUV;
            } catch (jakarta.persistence.NoResultException e) {
                // 如果没有结果，返回 0.0
                return 0.0;
            }
        } catch (Exception e) {
            // 记录错误并返回默认值，避免接口崩溃
            org.slf4j.LoggerFactory.getLogger(AnalyticsService.class)
                    .error("Error calculating bounce rate", e);
            return 0.0;
        }
    }

    /**
     * 计算平均停留时长（秒）
     */
    public double getAvgDuration(String appId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Specification<Event> spec = Specification.where(null);

            if (appId != null && !appId.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("appId"), appId));
            }

            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventTypeId"), EventType.PAGE_STAY.getCode()))
                    .and((root, query, cb) -> cb.isNotNull(root.get("properties")));

            // 只有当提供了时间范围时才添加时间条件
            if (startTime != null && endTime != null) {
                spec = spec.and((root, query, cb) -> cb.between(root.get("serverTimestamp"), startTime, endTime));
            }

            List<Event> events = eventRepository.findAll(spec);

            if (events.isEmpty()) {
                return 0.0;
            }

            double sum = 0.0;
            int count = 0;

            for (Event event : events) {
                if (event.getProperties() != null && event.getProperties().containsKey("duration")) {
                    Object durationObj = event.getProperties().get("duration");
                    if (durationObj instanceof Number) {
                        sum += ((Number) durationObj).doubleValue();
                        count++;
                    }
                }
            }

            return count > 0 ? sum / count : 0.0;
        } catch (Exception e) {
            // 记录错误并返回默认值，避免接口崩溃
            org.slf4j.LoggerFactory.getLogger(AnalyticsService.class)
                    .error("Error calculating average duration", e);
            return 0.0;
        }
    }

    /**
     * 获取 PV/UV 时间序列
     */
    public List<PvUvSeriesResponse.TimeSeriesPoint> getPvUvSeries(
            String appId, LocalDateTime startTime, LocalDateTime endTime,
            String interval, ZoneId timezone) {

        String dateFormat;
        switch (interval.toLowerCase()) {
            case "minute":
                dateFormat = "YYYY-MM-DD HH24:MI";
                break;
            case "hour":
                dateFormat = "YYYY-MM-DD HH24:00";
                break;
            case "day":
                dateFormat = "YYYY-MM-DD";
                break;
            default:
                dateFormat = "YYYY-MM-DD HH24:00";
        }

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    TO_CHAR(server_timestamp AT TIME ZONE :timezone, :dateFormat) as ts,
                    COUNT(*) as pv,
                    COUNT(DISTINCT user_id) as uv
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                GROUP BY ts
                ORDER BY ts
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.PAGE_VIEW.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("timezone", timezone.getId());
        query.setParameter("dateFormat", dateFormat);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new PvUvSeriesResponse.TimeSeriesPoint(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取页面 TopN
     */
    public List<PagesTopResponse.PageStats> getPagesTop(
            String appId, LocalDateTime startTime, LocalDateTime endTime, int limit) {

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    page_url,
                    COUNT(*) as pv,
                    COUNT(DISTINCT user_id) as uv,
                    COALESCE(AVG(
                        CASE
                            WHEN (properties->>'duration') ~ '^[0-9]+(\\.[0-9]+)?$'
                            THEN CAST((properties->>'duration') AS numeric)
                            ELSE NULL
                        END
                    ), 0) as avg_duration
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                  AND page_url IS NOT NULL
                GROUP BY page_url
                ORDER BY pv DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.PAGE_VIEW.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new PagesTopResponse.PageStats(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取事件类型分布
     */
    public List<EventsDistributionResponse.TypeDistribution> getEventsDistribution(
            String appId, LocalDateTime startTime, LocalDateTime endTime) {

        String timeCondition = (startTime != null && endTime != null)
                ? "AND e.server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    CASE
                        WHEN event_type_id = 5 THEN 'custom'
                        ELSE et.name
                    END as type,
                    COUNT(*) as value
                FROM events e
                LEFT JOIN event_types et ON e.event_type_id = et.id
                WHERE 1=1
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND e.app_id = :appId\n" : "") + """
                """ + timeCondition + """
                GROUP BY type
                ORDER BY value DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new EventsDistributionResponse.TypeDistribution(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取 Web Vitals 分位统计
     */
    public WebVitalsResponse getWebVitals(
            String appId, LocalDateTime startTime, LocalDateTime endTime, String metric) {

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p50,
                    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p75,
                    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p95
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                  AND properties->>:metric IS NOT NULL
                  AND (properties->>:metric) ~ '^[0-9]+(\\.[0-9]+)?$'
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.PERFORMANCE.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("metric", metric.toLowerCase());

        Object[] result = (Object[]) query.getSingleResult();

        return new WebVitalsResponse(
                result[0] != null ? ((Number) result[0]).doubleValue() : 0.0,
                result[1] != null ? ((Number) result[1]).doubleValue() : 0.0,
                result[2] != null ? ((Number) result[2]).doubleValue() : 0.0,
                "ms");
    }

    /**
     * 获取 Web Vitals 趋势
     */
    public List<WebVitalsSeriesResponse.WebVitalsPoint> getWebVitalsSeries(
            String appId, LocalDateTime startTime, LocalDateTime endTime,
            String metric, String interval, ZoneId timezone) {

        String dateFormat;
        switch (interval.toLowerCase()) {
            case "hour":
                dateFormat = "YYYY-MM-DD HH24:00";
                break;
            case "day":
                dateFormat = "YYYY-MM-DD";
                break;
            default:
                dateFormat = "YYYY-MM-DD HH24:00";
        }

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    TO_CHAR(server_timestamp AT TIME ZONE :timezone, :dateFormat) as ts,
                    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p50,
                    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p75,
                    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CAST((properties->>:metric) AS numeric)) as p95
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                  AND properties->>:metric IS NOT NULL
                  AND (properties->>:metric) ~ '^[0-9]+(\\.[0-9]+)?$'
                GROUP BY ts
                ORDER BY ts
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.PERFORMANCE.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("metric", metric.toLowerCase());
        query.setParameter("timezone", timezone.getId());
        query.setParameter("dateFormat", dateFormat);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new WebVitalsSeriesResponse.WebVitalsPoint(
                        (String) row[0],
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                        row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                        row[3] != null ? ((Number) row[3]).doubleValue() : 0.0))
                .collect(Collectors.toList());
    }

    /**
     * 获取自定义事件趋势
     */
    public List<CustomEventsSeriesResponse.CustomEventPoint> getCustomEventsSeries(
            String appId, String eventId, LocalDateTime startTime, LocalDateTime endTime,
            String groupBy, ZoneId timezone) {

        String dateFormat = "hour".equals(groupBy.toLowerCase()) ? "YYYY-MM-DD HH24:00" : "YYYY-MM-DD";

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    TO_CHAR(server_timestamp AT TIME ZONE :timezone, :dateFormat) as ts,
                    COUNT(*) as count
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                  """ + timeCondition + """
                """ + (eventId != null && !eventId.isEmpty() ? "AND custom_event_id = :eventId" : "") + """
                GROUP BY ts
                ORDER BY ts
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.CUSTOM.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("timezone", timezone.getId());
        query.setParameter("dateFormat", dateFormat);
        if (eventId != null && !eventId.isEmpty()) {
            query.setParameter("eventId", eventId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new CustomEventsSeriesResponse.CustomEventPoint(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取自定义事件总数
     */
    public long getCustomEventsTotal(String appId, String eventId,
            LocalDateTime startTime, LocalDateTime endTime) {
        Specification<Event> spec = Specification.where(null);

        if (appId != null && !appId.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("appId"), appId));
        }

        spec = spec.and((root, query, cb) -> cb.equal(root.get("eventTypeId"), EventType.CUSTOM.getCode()));

        // 只有当提供了时间范围时才添加时间条件
        if (startTime != null && endTime != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("serverTimestamp"), startTime, endTime));
        }

        if (eventId != null && !eventId.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customEventId"), eventId));
        }

        return eventRepository.count(spec);
    }

    /**
     * 获取自定义事件 TopN
     */
    public List<CustomEventsTopResponse.CustomEventStats> getCustomEventsTop(
            String appId, LocalDateTime startTime, LocalDateTime endTime, int limit) {

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    custom_event_id as event_id,
                    COUNT(*) as count
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                  AND custom_event_id IS NOT NULL
                GROUP BY custom_event_id
                ORDER BY count DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.CUSTOM.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new CustomEventsTopResponse.CustomEventStats(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取错误趋势
     */
    public List<ErrorsTrendResponse.ErrorPoint> getErrorsTrend(
            String appId, LocalDateTime startTime, LocalDateTime endTime,
            String interval, ZoneId timezone) {

        String dateFormat;
        switch (interval.toLowerCase()) {
            case "hour":
                dateFormat = "YYYY-MM-DD HH24:00";
                break;
            case "day":
                dateFormat = "YYYY-MM-DD";
                break;
            default:
                dateFormat = "YYYY-MM-DD HH24:00";
        }

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    TO_CHAR(server_timestamp AT TIME ZONE :timezone, :dateFormat) as ts,
                    COUNT(*) as count
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                GROUP BY ts
                ORDER BY ts
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.ERROR.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("timezone", timezone.getId());
        query.setParameter("dateFormat", dateFormat);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new ErrorsTrendResponse.ErrorPoint(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取错误 TopN
     * 错误指纹基于 message + stack（如果存在）
     */
    public List<ErrorsTopResponse.ErrorStats> getErrorsTop(
            String appId, LocalDateTime startTime, LocalDateTime endTime, int limit) {

        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT
                    COALESCE(
                        MD5(COALESCE(properties->>'message', '') || COALESCE(properties->>'stack', '')),
                        MD5(COALESCE(properties->>'message', ''))
                    ) as fingerprint,
                    properties->>'message' as message,
                    COUNT(*) as count,
                    MIN(server_timestamp) as first_seen,
                    MAX(server_timestamp) as last_seen
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                  """ + timeCondition + """
                GROUP BY fingerprint, message
                ORDER BY count DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.ERROR.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return results.stream()
                .map(row -> new ErrorsTopResponse.ErrorStats(
                        (String) row[0],
                        row[1] != null ? (String) row[1] : "",
                        ((Number) row[2]).longValue(),
                        row[3] != null ? ((LocalDateTime) row[3]).format(formatter) : "",
                        row[4] != null ? ((LocalDateTime) row[4]).format(formatter) : ""))
                .collect(Collectors.toList());
    }

    /**
     * 获取页面 TopN 总数
     */
    public long getPagesTopTotal(String appId, LocalDateTime startTime, LocalDateTime endTime) {
        String timeCondition = (startTime != null && endTime != null)
                ? "AND server_timestamp BETWEEN :startTime AND :endTime"
                : "";

        String sql = """
                SELECT COUNT(DISTINCT page_url)
                FROM events
                WHERE event_type_id = :eventTypeId
                """ + (appId != null && !appId.isEmpty() ? "\n                  AND app_id = :appId\n" : "") + """
                """ + timeCondition + """
                  AND page_url IS NOT NULL
                """;

        Query query = entityManager.createNativeQuery(sql);
        if (appId != null && !appId.isEmpty()) {
            query.setParameter("appId", appId);
        }
        query.setParameter("eventTypeId", EventType.PAGE_VIEW.getCode());
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }

        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }
}
