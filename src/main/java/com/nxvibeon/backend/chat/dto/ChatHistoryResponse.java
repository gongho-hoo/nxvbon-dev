package com.nxvibeon.backend.chat.dto;

import com.nxvibeon.backend.chat.domain.ChatMessageHistoryEntity;
import com.nxvibeon.backend.chat.domain.ChatRole;

import java.time.OffsetDateTime;

public record ChatHistoryResponse(
    String id,
    String projectId,
    String sessionId,
    ChatRole role,
    String content,
    String metadataJson,
    OffsetDateTime createdAt
) {
    public static ChatHistoryResponse from(ChatMessageHistoryEntity entity) {
        return new ChatHistoryResponse(
            entity.getId(),
            entity.getProjectId(),
            entity.getSessionId(),
            entity.getRole(),
            entity.getContent(),
            entity.getMetadataJson(),
            entity.getCreatedAt()
        );
    }
}
