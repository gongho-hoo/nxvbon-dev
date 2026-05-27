package com.nxvibeon.backend.chat.service;

import com.nxvibeon.backend.chat.domain.ChatMessageHistoryEntity;
import com.nxvibeon.backend.chat.domain.ChatRole;
import com.nxvibeon.backend.chat.dto.ChatHistoryResponse;
import com.nxvibeon.backend.chat.dto.ChatHistorySaveRequest;
import com.nxvibeon.backend.chat.dto.ChatSessionSummaryResponse;
import com.nxvibeon.backend.chat.repository.ChatMessageHistoryJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ChatHistoryService {
    private final ChatMessageHistoryJpaRepository repository;

    public ChatHistoryService(ChatMessageHistoryJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ChatHistoryResponse save(ChatHistorySaveRequest request) {
        return save(request.projectId(), request.sessionId(), request.role(), request.content(), request.metadataJson());
    }

    @Transactional
    public ChatHistoryResponse save(String projectId, String sessionId, ChatRole role, String content, String metadataJson) {
        ChatMessageHistoryEntity entity = ChatMessageHistoryEntity.create(normalizeProjectId(projectId), sessionId, role, content, metadataJson);
        return ChatHistoryResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> findBySession(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
            .filter(item -> item.getProjectId() == null)
            .map(ChatHistoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> findByProjectAndSession(String projectId, String sessionId) {
        return repository.findByProjectIdAndSessionIdOrderByCreatedAtAsc(projectId, sessionId).stream()
            .map(ChatHistoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> findRecentByProject(String projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<ChatMessageHistoryEntity> rows;
        if (StringUtils.hasText(projectId)) {
            rows = repository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, safeLimit));
        } else {
            rows = repository.findByProjectIdIsNullOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        }
        return rows.stream().map(ChatHistoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> findProjectSessionSummaries(String projectId, int limit) {
        int safeLimit = safeSummaryLimit(limit);
        return repository.findProjectSessionSummaries(projectId, PageRequest.of(0, safeLimit)).stream()
            .map(row -> toSessionSummary(row.getProjectId(), row))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> findGeneralSessionSummaries(int limit) {
        int safeLimit = safeSummaryLimit(limit);
        return repository.findGeneralSessionSummaries(PageRequest.of(0, safeLimit)).stream()
            .map(row -> toSessionSummary(null, row))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> findRecentSessionSummaries(int limit) {
        int safeLimit = safeSummaryLimit(limit);
        return repository.findRecentSessionSummaries(PageRequest.of(0, safeLimit)).stream()
            .map(row -> toSessionSummary(row.getProjectId(), row))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> searchSessionSummaries(String keyword, String projectId, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = safeSummaryLimit(limit);
        String normalizedKeyword = keyword.trim();

        if ("__GENERAL__".equals(projectId)) {
            return repository.searchGeneralSessionSummaries(normalizedKeyword, PageRequest.of(0, safeLimit)).stream()
                .map(row -> toSessionSummary(null, row))
                .toList();
        }

        if (StringUtils.hasText(projectId)) {
            return repository.searchProjectSessionSummaries(projectId, normalizedKeyword, PageRequest.of(0, safeLimit)).stream()
                .map(row -> toSessionSummary(row.getProjectId(), row))
                .toList();
        }

        return repository.searchSessionSummaries(normalizedKeyword, PageRequest.of(0, safeLimit)).stream()
            .map(row -> toSessionSummary(row.getProjectId(), row))
            .toList();
    }

    @Transactional
    public long deleteProjectHistories(String projectId) {
        if (!StringUtils.hasText(projectId)) {
            return 0L;
        }
        return repository.deleteByProjectIdValue(projectId);
    }

    @Transactional
    public long deleteSession(String projectId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("삭제할 채팅 세션 ID가 필요합니다.");
        }
        return repository.deleteByProjectIdAndSessionIdNullable(normalizeProjectId(projectId), sessionId);
    }

    private int safeSummaryLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private ChatSessionSummaryResponse toSessionSummary(
        String projectId,
        ChatMessageHistoryJpaRepository.ChatSessionSummaryProjection row
    ) {
        String normalizedProjectId = normalizeProjectId(projectId);
        List<ChatMessageHistoryEntity> messages = StringUtils.hasText(normalizedProjectId)
            ? repository.findByProjectIdAndSessionIdOrderByCreatedAtAsc(normalizedProjectId, row.getSessionId())
            : repository.findBySessionIdOrderByCreatedAtAsc(row.getSessionId()).stream()
                .filter(item -> item.getProjectId() == null)
                .toList();

        String title = messages.stream()
            .filter(message -> message.getRole() == ChatRole.USER)
            .findFirst()
            .map(ChatMessageHistoryEntity::getContent)
            .orElseGet(() -> messages.isEmpty() ? row.getSessionId() : messages.get(0).getContent());

        return new ChatSessionSummaryResponse(
            normalizedProjectId,
            row.getSessionId(),
            summarizeTitle(title),
            row.getMessageCount(),
            row.getLastMessageAt()
        );
    }

    private String summarizeTitle(String value) {
        if (!StringUtils.hasText(value)) {
            return "새 채팅";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 40) {
            return normalized;
        }
        return normalized.substring(0, 40) + "...";
    }

    private String normalizeProjectId(String projectId) {
        return StringUtils.hasText(projectId) ? projectId : null;
    }
}
