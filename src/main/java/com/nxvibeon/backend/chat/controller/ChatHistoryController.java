package com.nxvibeon.backend.chat.controller;

import com.nxvibeon.backend.chat.dto.ChatHistoryResponse;
import com.nxvibeon.backend.chat.dto.ChatHistorySaveRequest;
import com.nxvibeon.backend.chat.dto.ChatSessionSummaryResponse;
import com.nxvibeon.backend.chat.service.ChatHistoryService;
import com.nxvibeon.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping("/api/v1/chat/history")
    public ApiResponse<ChatHistoryResponse> save(@Valid @RequestBody ChatHistorySaveRequest request) {
        return ApiResponse.ok(chatHistoryService.save(request));
    }

    @GetMapping("/api/v1/chat/history")
    public ApiResponse<List<ChatHistoryResponse>> findGeneralHistory(
        @RequestParam(required = false) String sessionId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        if (StringUtils.hasText(sessionId)) {
            return ApiResponse.ok(chatHistoryService.findBySession(sessionId));
        }
        return ApiResponse.ok(chatHistoryService.findRecentByProject(null, limit));
    }

    @GetMapping("/api/v1/chat/sessions")
    public ApiResponse<List<ChatSessionSummaryResponse>> findGeneralSessions(
        @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.ok(chatHistoryService.findGeneralSessionSummaries(limit));
    }

    @GetMapping("/api/v1/chat/recent")
    public ApiResponse<List<ChatSessionSummaryResponse>> findRecentSessions(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(chatHistoryService.findRecentSessionSummaries(limit));
    }

    @GetMapping("/api/v1/chat/search")
    public ApiResponse<List<ChatSessionSummaryResponse>> searchSessions(
        @RequestParam String keyword,
        @RequestParam(required = false) String projectId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(chatHistoryService.searchSessionSummaries(keyword, projectId, limit));
    }

    @DeleteMapping("/api/v1/chat/sessions/{sessionId}")
    public ApiResponse<Map<String, Long>> deleteSession(
        @PathVariable String sessionId,
        @RequestParam(required = false) String projectId
    ) {
        long deletedCount = chatHistoryService.deleteSession(projectId, sessionId);
        return ApiResponse.ok(Map.of("deletedCount", deletedCount));
    }

    @GetMapping("/api/v1/projects/{projectId}/chat/history")
    public ApiResponse<List<ChatHistoryResponse>> findProjectHistory(
        @PathVariable String projectId,
        @RequestParam(required = false) String sessionId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        if (StringUtils.hasText(sessionId)) {
            return ApiResponse.ok(chatHistoryService.findByProjectAndSession(projectId, sessionId));
        }
        return ApiResponse.ok(chatHistoryService.findRecentByProject(projectId, limit));
    }

    @GetMapping("/api/v1/projects/{projectId}/chat/sessions")
    public ApiResponse<List<ChatSessionSummaryResponse>> findProjectSessions(
        @PathVariable String projectId,
        @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.ok(chatHistoryService.findProjectSessionSummaries(projectId, limit));
    }
}
