package com.track.repository;

import com.track.entity.Event;
import com.track.entity.EventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 事件仓储接口
 * 注意：使用复合主键 EventId (id, app_id)
 */
@Repository
public interface EventRepository extends JpaRepository<Event, EventId>, JpaSpecificationExecutor<Event> {
}

