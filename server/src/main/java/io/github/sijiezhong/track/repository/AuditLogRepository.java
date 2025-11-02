package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}


