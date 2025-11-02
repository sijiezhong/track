package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findByTenantId(Integer tenantId);
}


