package com.nxvibeon.backend.project.service;

import com.nxvibeon.backend.chat.service.ChatHistoryService;
import com.nxvibeon.backend.project.domain.ProjectEntity;
import com.nxvibeon.backend.project.dto.CreateProjectRequest;
import com.nxvibeon.backend.project.dto.ProjectResponse;
import com.nxvibeon.backend.project.dto.UpdateProjectRequest;
import com.nxvibeon.backend.project.repository.ProjectJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectJpaRepository repository;
    private final ChatHistoryService chatHistoryService;

    public ProjectService(ProjectJpaRepository repository, ChatHistoryService chatHistoryService) {
        this.repository = repository;
        this.chatHistoryService = chatHistoryService;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        validateCreateRequest(request);

        ProjectEntity saved = repository.save(new ProjectEntity(
            request.name().trim(),
            normalizeNullable(request.description()),
            request.rootPath().trim(),
            normalizeNullable(request.backendPath()),
            normalizeNullable(request.fastapiPath()),
            normalizeNullable(request.webUiPath()),
            normalizeVcsType(request.vcsType()),
            normalizeNullable(request.vcsUrl()),
            normalizeDefaultBranch(request.defaultBranch())
        ));
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return repository.findByEnabledTrueOrderByCreatedAtDesc()
            .stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(String id) {
        return ProjectResponse.from(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    public ProjectEntity findActiveEntity(String id) {
        ProjectEntity project = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + id));

        if (!Boolean.TRUE.equals(project.getEnabled())) {
            throw new IllegalArgumentException("비활성화된 프로젝트입니다: " + id);
        }
        return project;
    }

    @Transactional(readOnly = true)
    public void validateProjectExistsIfPresent(String projectId) {
        if (!StringUtils.hasText(projectId)) {
            return;
        }
        findActiveEntity(projectId);
    }

    @Transactional
    public ProjectResponse update(String id, UpdateProjectRequest request) {
        ProjectEntity project = findActiveEntity(id);
        validateUpdateRequest(request);

        project.setName(request.name().trim());
        project.setDescription(normalizeNullable(request.description()));
        project.setRootPath(request.rootPath().trim());
        project.setBackendPath(normalizeNullable(request.backendPath()));
        project.setFastapiPath(normalizeNullable(request.fastapiPath()));
        project.setWebUiPath(normalizeNullable(request.webUiPath()));
        project.setVcsType(normalizeVcsType(request.vcsType()));
        project.setVcsUrl(normalizeNullable(request.vcsUrl()));
        project.setDefaultBranch(normalizeDefaultBranch(request.defaultBranch()));
        if (request.enabled() != null) {
            project.setEnabled(request.enabled());
        }

        return ProjectResponse.from(project);
    }

    /**
     * 프로젝트 삭제 정책:
     * - 해당 프로젝트의 채팅 이력을 먼저 물리 삭제합니다.
     * - projects 테이블의 프로젝트 row도 물리 삭제합니다.
     *
     * 주의:
     * - 기존 enabled=false 논리 삭제 방식은 사용하지 않습니다.
     * - 이후 scm_repositories, document_sets 등 project_id를 참조하는 테이블이 추가되면
     *   projects 삭제 전에 해당 자식 테이블도 먼저 삭제해야 합니다.
     */
    @Transactional
    public long deleteWithHistories(String id) {
        ProjectEntity project = findActiveEntity(id);

        log.info("프로젝트 물리 삭제 시작. projectId={}, projectName={}", project.getId(), project.getName());

        long deletedChatCount = chatHistoryService.deleteProjectHistories(id);

        repository.delete(project);
        repository.flush();

        log.info("프로젝트 물리 삭제 완료. projectId={}, deletedChatHistoryCount={}", id, deletedChatCount);

        return deletedChatCount;
    }

    @Transactional
    public void disable(String id) {
        deleteWithHistories(id);
    }

    private void validateCreateRequest(CreateProjectRequest request) {
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("프로젝트명은 필수입니다.");
        }
        if (!StringUtils.hasText(request.rootPath())) {
            throw new IllegalArgumentException("프로젝트 루트 경로는 필수입니다.");
        }
        if (repository.existsByNameIgnoreCaseAndEnabledTrue(request.name().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 프로젝트명입니다: " + request.name());
        }
    }

    private void validateUpdateRequest(UpdateProjectRequest request) {
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("프로젝트명은 필수입니다.");
        }
        if (!StringUtils.hasText(request.rootPath())) {
            throw new IllegalArgumentException("프로젝트 루트 경로는 필수입니다.");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeVcsType(String value) {
        if (!StringUtils.hasText(value)) {
            return "NONE";
        }
        String normalized = value.trim().toUpperCase();
        if (!List.of("GIT", "SVN", "NONE").contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 형상관리 유형입니다: " + value);
        }
        return normalized;
    }

    private String normalizeDefaultBranch(String value) {
        return StringUtils.hasText(value) ? value.trim() : "main";
    }
}
