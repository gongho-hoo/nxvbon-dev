package com.nxvibeon.backend.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.ai.dto.AiChatRequest;
import com.nxvibeon.backend.ai.dto.AiChatResponse;
import com.nxvibeon.backend.chat.domain.ChatRole;
import com.nxvibeon.backend.chat.service.ChatHistoryService;
import com.nxvibeon.backend.common.ApiResponse;
import com.nxvibeon.backend.logging.JsonLogMasking;
import com.nxvibeon.backend.project.service.ProjectService;
import com.nxvibeon.backend.prompt.PromptTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/v1/chat")
public class ChatQueryController {
    private static final Logger log = LoggerFactory.getLogger(ChatQueryController.class);

    private final FastApiAiClient aiClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptTemplateService promptTemplateService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ChatQueryController(
        FastApiAiClient aiClient,
        ChatHistoryService chatHistoryService,
        PromptTemplateService promptTemplateService,
        ProjectService projectService,
        ObjectMapper objectMapper
    ) {
        this.aiClient = aiClient;
        this.chatHistoryService = chatHistoryService;
        this.promptTemplateService = promptTemplateService;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/query")
    public ApiResponse<AiChatResponse> query(@Valid @RequestBody AiChatRequest request) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : UUID.randomUUID().toString();
        String projectId = normalizeProjectId(request.projectId());

        if (projectId != null) {
            projectService.validateProjectExistsIfPresent(projectId);
        }

        log.info("채팅 요청 수신. mode={}, projectId={}, sessionId={}", projectId == null ? "GENERAL" : "PROJECT", projectId, sessionId);
        if (log.isDebugEnabled()) {
            log.debug("채팅 요청 받은 내용: {}", JsonLogMasking.toSafeJson(objectMapper, request));
        }

        chatHistoryService.save(projectId, sessionId, ChatRole.USER, request.userMessage(), null);

        AiChatRequest enrichedRequest = new AiChatRequest(
            projectId,
            sessionId,
            request.userMessage(),
            enrichContextHints(request, projectId)
        );

        if (log.isDebugEnabled()) {
            log.debug("채팅 FastAPI로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, enrichedRequest));
        }

        AiChatResponse response = aiClient.ragQuery(enrichedRequest);
        if (response != null && StringUtils.hasText(response.answer())) {
            chatHistoryService.save(projectId, sessionId, ChatRole.ASSISTANT, response.answer(), null);
        }

        if (log.isDebugEnabled()) {
            log.debug("채팅 Web UI로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, response));
        }

        log.info("채팅 요청 완료. mode={}, projectId={}, sessionId={}", projectId == null ? "GENERAL" : "PROJECT", projectId, sessionId);
        return ApiResponse.ok(response);
    }

    private String normalizeProjectId(String rawProjectId) {
        if (!StringUtils.hasText(rawProjectId)) {
            return null;
        }
        String projectId = rawProjectId.trim();
        if (projectId.startsWith("temp-project")) {
            throw new IllegalArgumentException("임시 프로젝트 ID는 사용할 수 없습니다. 프로젝트 생성 API로 실제 프로젝트를 생성한 뒤 다시 시도하세요: " + projectId);
        }
        return projectId;
    }

    private List<Map<String, Object>> enrichContextHints(AiChatRequest request, String projectId) {
        List<Map<String, Object>> hints = new ArrayList<>();
        if (request.contextHints() != null) {
            hints.addAll(request.contextHints());
        }

        Map<String, Object> promptHint = new LinkedHashMap<>();
        promptHint.put("type", "systemPrompt");
        promptHint.put("language", "ko");
        promptHint.put("chatMode", projectId == null ? "GENERAL" : "PROJECT");
        promptHint.put("content", promptTemplateService.resolveChatPrompt(projectId));
        hints.add(0, promptHint);

        return hints;
    }
}
