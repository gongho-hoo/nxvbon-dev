package com.nxvibeon.backend.project.dto;

import com.nxvibeon.backend.project.domain.ProjectEntity;

import java.time.Instant;

public record ProjectResponse(
    String id,
    String projectId,
    String name,
    String description,
    String rootPath,
    String backendPath,
    String fastapiPath,
    String webUiPath,
    String repositoryPath,
    String vcsType,
    String vcsUrl,
    String defaultBranch,
    Boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProjectResponse from(ProjectEntity project) {
        return new ProjectResponse(
            project.getId(),
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getRootPath(),
            project.getBackendPath(),
            project.getFastapiPath(),
            project.getWebUiPath(),
            project.getRootPath(),
            project.getVcsType(),
            project.getVcsUrl(),
            project.getDefaultBranch(),
            project.getEnabled(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
