package com.track.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Event 实体的复合主键
 * 用于支持 PostgreSQL 分区表（主键必须包含分区键 app_id）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventId implements Serializable {
    private Long id;
    private String appId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventId eventId = (EventId) o;
        return Objects.equals(id, eventId.id) && Objects.equals(appId, eventId.appId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, appId);
    }
}

