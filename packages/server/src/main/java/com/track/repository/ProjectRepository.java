package com.track.repository;

import com.track.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 项目仓储接口
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    /**
     * 根据 appId 查找项目
     */
    Optional<Project> findByAppId(String appId);
}

