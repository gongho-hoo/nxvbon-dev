package com.nxvibeon.backend.ai.controller;

import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.ai.dto.AiChatRequest;
import com.nxvibeon.backend.ai.dto.AiChatResponse;
import com.nxvibeon.backend.chat.domain.ChatRole;
import com.nxvibeon.backend.chat.service.ChatHistoryService;
import com.nxvibeon.backend.common.ApiResponse;
import com.nxvibeon.backend.prompt.PromptTemplateService;
import com.nxvibeon.backend.logging.JsonLogMasking;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {
    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final FastApiAiClient aiClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public RagController(FastApiAiClient aiClient, ChatHistoryService chatHistoryService, PromptTemplateService promptTemplateService, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.chatHistoryService = chatHistoryService;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/query")
    public ApiResponse<AiChatResponse> query(@Valid @RequestBody AiChatRequest request) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : UUID.randomUUID().toString();
        String projectId = StringUtils.hasText(request.projectId()) ? request.projectId() : null;

        log.debug("RAG 요청 받은 내용: {}", JsonLogMasking.toSafeJson(objectMapper, request));

        chatHistoryService.save(projectId, sessionId, ChatRole.USER, request.userMessage(), null);

        AiChatRequest enrichedRequest = new AiChatRequest(
            projectId,
            sessionId,
            request.userMessage(),
            enrichContextHints(request)
        );

        log.debug("RAG FastAPI로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, enrichedRequest));

        AiChatResponse response = aiClient.ragQuery(enrichedRequest);
        if (response != null && StringUtils.hasText(response.answer())) {
            chatHistoryService.save(projectId, sessionId, ChatRole.ASSISTANT, response.answer(), null);
        }

        log.debug("RAG Web UI로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, response));

        return ApiResponse.ok(response);
    }

    private List<Map<String, Object>> enrichContextHints(AiChatRequest request) {
        List<Map<String, Object>> hints = new ArrayList<>();
        if (request.contextHints() != null) {
            hints.addAll(request.contextHints());
        }

        Map<String, Object> promptHint = new LinkedHashMap<>();
        promptHint.put("type", "systemPrompt");
        promptHint.put("language", "ko");
        promptHint.put("content", promptTemplateService.resolveChatPrompt(request.projectId()));
        hints.add(0, promptHint);

        return hints;
    }
}
