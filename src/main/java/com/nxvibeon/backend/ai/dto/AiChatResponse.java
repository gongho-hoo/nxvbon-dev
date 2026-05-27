package com.nxvibeon.backend.ai.dto;

import java.util.List;
import java.util.Map;

public record AiChatResponse(
    String answer,
    List<Map<String, Object>> sources,
    Map<String, Object> usage
) {
}
