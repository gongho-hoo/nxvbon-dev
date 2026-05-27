package com.nxvibeon.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank(message = "프로젝트명은 필수입니다.")
    @Size(max = 200, message = "프로젝트명은 200자를 초과할 수 없습니다.")
    String name,

    String description,

    @NotBlank(message = "프로젝트 루트 경로는 필수입니다.")
    String rootPath,

    String backendPath,
    String fastapiPath,
    String webUiPath,
    String vcsType,
    String vcsUrl,
    String defaultBranch
) {
}
