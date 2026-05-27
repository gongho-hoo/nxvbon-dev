package com.nxvibeon.backend.project.repository;

import com.nxvibeon.backend.project.domain.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findByEnabledTrueOrderByCreatedAtDesc();
    boolean existsByNameIgnoreCaseAndEnabledTrue(String name);
}
