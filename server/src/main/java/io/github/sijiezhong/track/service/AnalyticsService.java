package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.constants.BusinessConstants;
import io.github.sijiezhong.track.exception.BusinessException;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据分析服务
 * 
 * <p>
 * 提供事件数据的统计分析功能，包括趋势、路径、留存、漏斗、分群和热点分析。
 * 
 * @author sijie
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final EventRepository eventRepository;

    public AnalyticsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Map<String, Object>> trendDaily(Integer tenantId, String eventName, LocalDateTime start,
            LocalDateTime end) {
        List<Object[]> rows = eventRepository.aggregateDaily(tenantId, eventName, start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", r[0] == null ? null : r[0].toString());
            m.put("count", ((Number) r[1]).longValue());
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> pathEdges(Integer tenantId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = eventRepository.aggregatePathEdges(tenantId, start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("from", r[0] == null ? null : r[0].toString());
            m.put("to", r[1] == null ? null : r[1].toString());
            m.put("count", ((Number) r[2]).longValue());
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> retentionDaily(Integer tenantId, String cohortEvent, String returnEvent,
            Integer day, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = eventRepository.aggregateDailyRetention(tenantId, cohortEvent, returnEvent, day, start,
                end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            long cohort = ((Number) r[1]).longValue();
            long retained = ((Number) r[2]).longValue();
            m.put("cohortDate", r[0] == null ? null : r[0].toString());
            m.put("cohort", cohort);
            m.put("retained", retained);
            m.put("rate", cohort == 0 ? 0.0 : (retained * 1.0 / cohort));
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> funnel(Integer tenantId, List<String> steps, LocalDateTime start, LocalDateTime end) {
        log.debug("执行漏斗分析: tenantId={}, steps={}", tenantId, steps);

        if (steps == null || steps.size() < BusinessConstants.MIN_FUNNEL_STEPS) {
            throw new BusinessException(ErrorCode.FUNNEL_STEPS_INVALID,
                    String.format("漏斗步骤数量必须大于等于%d", BusinessConstants.MIN_FUNNEL_STEPS));
        }
        String[] stepArray = steps.toArray(new String[0]);
        List<Object[]> rows = eventRepository.findEventsForFunnel(tenantId, stepArray, start, end);

        // counts[i] 表示到达第 i 步（包含顺序约束）的会话数
        int n = stepArray.length;
        int[] counts = new int[n];

        // sessionId -> 当前已达成的步骤索引（-1 初始，达成第0步则为0 ...）
        Map<Long, Integer> sessionProgress = new LinkedHashMap<>();

        for (Object[] r : rows) {
            Long sessionId = r[0] == null ? null : ((Number) r[0]).longValue();
            String eventName = Objects.toString(r[1], null);
            if (sessionId == null || eventName == null)
                continue;
            Integer progress = sessionProgress.getOrDefault(sessionId, -1);

            // 下一个目标步骤索引
            int next = progress + 1;
            if (next < n && stepArray[next].equals(eventName)) {
                // 达成下一步
                sessionProgress.put(sessionId, next);
            }
        }

        // 统计每个会话最终达成的最高步骤，并累计各步数量（达到第k步意味着也必然达成[0..k]步）
        for (Integer p : sessionProgress.values()) {
            if (p >= 0) {
                for (int i = 0; i <= p; i++)
                    counts[i]++;
            }
        }

        double[] rates = new double[n];
        if (counts[0] > 0) {
            rates[0] = 1.0;
            for (int i = 1; i < n; i++) {
                rates[i] = counts[i - 1] == 0 ? 0.0 : (counts[i] * 1.0 / counts[i - 1]);
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("steps", steps);
        List<Integer> countList = new ArrayList<>(n);
        for (int c : counts)
            countList.add(c);
        out.put("counts", countList);
        List<Double> rateList = new ArrayList<>(n);
        for (double d : rates)
            rateList.add(d);
        out.put("conversionRates", rateList);
        return out;
    }

    public Map<String, Object> segmentation(Integer tenantId, String eventName, String by) {
        String column;
        switch (by) {
            case "browser":
                column = "browser";
                break;
            case "device":
                column = "device";
                break;
            case "os":
                column = "os";
                break;
            case "referrer":
                column = "referrer";
                break;
            default:
                throw new BusinessException(ErrorCode.SEGMENTATION_BY_INVALID, "不支持的分组维度: " + by);
        }
        List<Object[]> rows = eventRepository.segmentCountByColumn(tenantId, eventName, column);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("key", r[0]);
            m.put("count", ((Number) r[1]).longValue());
            items.add(m);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("by", by);
        out.put("items", items);
        return out;
    }

    public Map<String, Object> heatmap(Integer tenantId, String eventName, String bucket) {
        log.debug("执行热点分析: tenantId={}, eventName={}, bucket={}", tenantId, eventName, bucket);

        if (!"hour".equalsIgnoreCase(bucket)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目前仅支持hour粒度");
        }
        List<Object[]> rows = eventRepository.aggregateByHour(tenantId, eventName);
        long[] buckets = new long[24];
        for (Object[] r : rows) {
            int hour = ((Number) r[0]).intValue();
            long cnt = ((Number) r[1]).longValue();
            if (hour >= 0 && hour < 24)
                buckets[hour] = cnt;
        }
        Map<String, Object> out = new HashMap<>();
        List<Long> list = new ArrayList<>(24);
        for (int i = 0; i < 24; i++)
            list.add(buckets[i]);
        out.put("buckets", list);
        out.put("bucket", "hour");
        return out;
    }
}
