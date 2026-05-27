package com.nxvibeon.backend.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_message_histories")
public class ChatMessageHistoryEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 64)
    private String projectId;

    @Column(nullable = false, length = 128)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String metadataJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static ChatMessageHistoryEntity create(String projectId, String sessionId, ChatRole role, String content, String metadataJson) {
        ChatMessageHistoryEntity entity = new ChatMessageHistoryEntity();
        entity.id = UUID.randomUUID().toString();
        entity.projectId = projectId;
        entity.sessionId = sessionId;
        entity.role = role;
        entity.content = content;
        entity.metadataJson = metadataJson;
        entity.createdAt = OffsetDateTime.now();
        return entity;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getSessionId() { return sessionId; }
    public ChatRole getRole() { return role; }
    public String getContent() { return content; }
    public String getMetadataJson() { return metadataJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
