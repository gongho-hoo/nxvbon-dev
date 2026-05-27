package com.nxvibeon.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record AiChatRequest(
    String chatMode,
    String workMode,
    String projectId,
    String sessionId,
    @NotBlank String userMessage,
    List<Map<String, Object>> contextHints,
    Map<String, Object> selectedSource
) {
    public AiChatRequest(String projectId, String sessionId, String userMessage, List<Map<String, Object>> contextHints) {
        this(null, null, projectId, sessionId, userMessage, contextHints, null);
    }
}
