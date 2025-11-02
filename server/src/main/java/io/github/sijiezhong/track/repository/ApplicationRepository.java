package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findByTenantId(Integer tenantId);
}


