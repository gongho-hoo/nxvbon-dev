package com.nxvibeon.backend.chat.dto;

import java.time.OffsetDateTime;

public record ChatSessionSummaryResponse(
    String projectId,
    String sessionId,
    String title,
    long messageCount,
    OffsetDateTime lastMessageAt
) {
}
