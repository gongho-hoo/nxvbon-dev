package com.nxvibeon.backend.codechange.repository;

import com.nxvibeon.backend.codechange.domain.CodeChangeHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeChangeHistoryJpaRepository extends JpaRepository<CodeChangeHistoryEntity, String> {
    List<CodeChangeHistoryEntity> findTop3ByProjectIdAndTargetFilePathOrderByCreatedAtDesc(String projectId, String targetFilePath);
    List<CodeChangeHistoryEntity> findByProjectIdAndTargetFilePathOrderByCreatedAtDesc(String projectId, String targetFilePath);
}
