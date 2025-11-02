package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件仓库
 */
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    @Query(value = "select date_trunc('day', event_time) as d, count(*) as c " +
            "from event e where e.tenant_id = :tenantId and e.event_name = :eventName " +
            "and e.event_time between coalesce(:startTime, '-infinity'::timestamp) and coalesce(:endTime, 'infinity'::timestamp) " +
            "group by d order by d", nativeQuery = true)
    List<Object[]> aggregateDaily(@Param("tenantId") Integer tenantId,
                                  @Param("eventName") String eventName,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    @Query(value = "select prev as from, curr as to, count(*) as cnt from (" +
            "  select lag(e.event_name) over (partition by e.session_id order by e.event_time) as prev, " +
            "         e.event_name as curr " +
            "  from event e " +
            "  where e.tenant_id = :tenantId " +
            "    and e.event_time between coalesce(:startTime, '-infinity'::timestamp) and coalesce(:endTime, 'infinity'::timestamp)" +
            ") t where prev is not null group by prev, curr order by cnt desc", nativeQuery = true)
    List<Object[]> aggregatePathEdges(@Param("tenantId") Integer tenantId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    @Query(value = "select date_trunc('day', cohort.event_time) as cohort_day, " +
            "count(distinct cohort.user_id) as cohort, " +
            "count(distinct ret.user_id) as retained " +
            "from event cohort " +
            "left join event ret on ret.tenant_id = cohort.tenant_id and ret.user_id = cohort.user_id " +
            "  and ret.event_name = :returnEvent " +
            "  and date_trunc('day', ret.event_time) = date_trunc('day', cohort.event_time) + (cast(:day as text) || ' day')::interval " +
            "where cohort.tenant_id = :tenantId and cohort.event_name = :cohortEvent " +
            "  and cohort.event_time between coalesce(:startTime, '-infinity'::timestamp) and coalesce(:endTime, 'infinity'::timestamp) " +
            "group by cohort_day order by cohort_day", nativeQuery = true)
    List<Object[]> aggregateDailyRetention(@Param("tenantId") Integer tenantId,
                                           @Param("cohortEvent") String cohortEvent,
                                           @Param("returnEvent") String returnEvent,
                                           @Param("day") Integer day,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    @Query(value = "select e.session_id, e.event_name, e.event_time from event e " +
            "where e.tenant_id = :tenantId " +
            "  and e.event_name = any(:steps) " +
            "  and e.event_time between coalesce(:startTime, '-infinity'::timestamp) and coalesce(:endTime, 'infinity'::timestamp) " +
            "order by e.session_id, e.event_time", nativeQuery = true)
    List<Object[]> findEventsForFunnel(@Param("tenantId") Integer tenantId,
                                       @Param("steps") String[] steps,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    @Query(value = "select * from event e where e.tenant_id = :tenantId order by e.event_time desc limit 1", nativeQuery = true)
    List<Event> findLatestByTenant(@Param("tenantId") Integer tenantId);

    @Query(value = "select " +
            "  coalesce( case when :column = 'browser' then e.browser " +
            "                 when :column = 'device' then e.device " +
            "                 when :column = 'os' then e.os " +
            "                 when :column = 'referrer' then e.referrer end, 'unknown') as key, " +
            "  count(*) as c " +
            "from event e where e.tenant_id = :tenantId and e.event_name = :eventName " +
            "group by key order by c desc", nativeQuery = true)
    List<Object[]> segmentCountByColumn(@Param("tenantId") Integer tenantId,
                                        @Param("eventName") String eventName,
                                        @Param("column") String column);

    @Query(value = "select extract(hour from e.event_time)::int as h, count(*) as c " +
            "from event e where e.tenant_id = :tenantId and e.event_name = :eventName " +
            "group by h order by h", nativeQuery = true)
    List<Object[]> aggregateByHour(@Param("tenantId") Integer tenantId,
                                   @Param("eventName") String eventName);
}
