package com.nxvibeon.backend.codechange.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.ai.dto.AiChatRequest;
import com.nxvibeon.backend.ai.dto.AiChatResponse;
import com.nxvibeon.backend.codechange.dto.ApplyCodeChangeResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeHistoryResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalRequest;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalResponse;

/**
 * Workspace 기반 기능변경 서비스.
 *
 * 중요:
 * - targetFilePath로 서버 파일 시스템을 읽지 않습니다.
 * - 원본 코드는 Web UI Workspace 창에서 전달받은 originalCode를 사용합니다.
 * - 변경 코드는 FastAPI/LLM에 originalCode + instruction을 전달해서 생성합니다.
 * - apply/rollback은 Backend 파일 시스템에 직접 저장하지 않고, 응답으로 proposedCode/beforeCode를 내려줍니다.
 *   실제 Workspace 반영은 Web UI가 수행하고, 실제 파일 저장은 Workspace 저장 버튼에서 수행합니다.
 */
@Service
public class WorkspaceCodeChangeService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceCodeChangeService.class);
    private static final int MAX_HISTORY_PER_FILE = 3;
    private static final int MAX_SOURCE_LENGTH = 300_000;

    private final UnifiedDiffService unifiedDiffService;
    private final FastApiAiClient fastApiAiClient;

    private final ConcurrentMap<String, ProposalState> proposals = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HistoryState> histories = new ConcurrentHashMap<>();

    public WorkspaceCodeChangeService(
        UnifiedDiffService unifiedDiffService,
        FastApiAiClient fastApiAiClient
    ) {
        this.unifiedDiffService = unifiedDiffService;
        this.fastApiAiClient = fastApiAiClient;
    }

    @Transactional
    public CodeChangeProposalResponse createProposal(CodeChangeProposalRequest request) {
        validateProposalRequest(request);

        String proposalId = "proposal-" + UUID.randomUUID();
        String proposedCode = generateProposedCodeByLlm(request);
        String diffText = unifiedDiffService.createUnifiedDiff(
            request.targetFilePath(),
            request.originalCode(),
            proposedCode
        );

        ProposalState proposal = new ProposalState(
            proposalId,
            request.projectId(),
            request.sessionId(),
            request.targetFilePath(),
            request.instruction(),
            request.originalCode(),
            proposedCode,
            diffText,
            sha256(request.originalCode()),
            sha256(proposedCode),
            "PENDING",
            LocalDateTime.now()
        );

        proposals.put(proposalId, proposal);

        log.info("Workspace 기능변경 제안 생성 완료. proposalId={}, projectId={}, targetFilePath={}",
            proposalId,
            request.projectId(),
            request.targetFilePath()
        );

        return proposal.toResponse();
    }

    @Transactional(readOnly = true)
    public CodeChangeProposalResponse getProposal(String proposalId) {
        return getProposalState(proposalId).toResponse();
    }

    @Transactional
    public ApplyCodeChangeResponse applyProposal(String proposalId) {
        ProposalState proposal = getProposalState(proposalId);

        if (!"PENDING".equals(proposal.status())) {
            throw new IllegalStateException("적용 가능한 상태가 아닙니다. proposalId=" + proposalId + ", status=" + proposal.status());
        }

        String historyId = "history-" + UUID.randomUUID();
        HistoryState history = new HistoryState(
            historyId,
            proposal.projectId(),
            proposal.proposalId(),
            proposal.targetFilePath(),
            proposal.instruction(),
            proposal.originalCode(),
            proposal.proposedCode(),
            "APPLY",
            LocalDateTime.now()
        );

        histories.put(historyId, history);
        proposals.put(proposalId, proposal.withStatus("APPLIED"));
        trimHistories(proposal.projectId(), proposal.targetFilePath());

        log.info("Workspace 기능변경 제안 적용 완료. proposalId={}, historyId={}, projectId={}, targetFilePath={}",
            proposalId,
            historyId,
            proposal.projectId(),
            proposal.targetFilePath()
        );

        return new ApplyCodeChangeResponse(
            proposalId,
            historyId,
            proposal.projectId(),
            proposal.targetFilePath(),
            proposal.proposedCode(),
            "APPLIED",
            LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<CodeChangeHistoryResponse> getHistories(String projectId, String targetFilePath) {
        return histories.values().stream()
            .filter(h -> equals(projectId, h.projectId()))
            .filter(h -> targetFilePath == null || targetFilePath.isBlank() || equals(targetFilePath, h.targetFilePath()))
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .limit(MAX_HISTORY_PER_FILE)
            .map(HistoryState::toResponse)
            .toList();
    }

    @Transactional
    public ApplyCodeChangeResponse rollback(String historyId) {
        HistoryState history = histories.get(historyId);
        if (history == null) {
            throw new IllegalArgumentException("변경 이력을 찾을 수 없습니다: " + historyId);
        }

        String rollbackHistoryId = "history-" + UUID.randomUUID();
        HistoryState rollbackHistory = new HistoryState(
            rollbackHistoryId,
            history.projectId(),
            history.proposalId(),
            history.targetFilePath(),
            "원복: " + history.instruction(),
            history.afterCode(),
            history.beforeCode(),
            "ROLLBACK",
            LocalDateTime.now()
        );
        histories.put(rollbackHistoryId, rollbackHistory);
        trimHistories(history.projectId(), history.targetFilePath());

        log.info("Workspace 기능변경 원복 완료. historyId={}, rollbackHistoryId={}, projectId={}, targetFilePath={}",
            historyId,
            rollbackHistoryId,
            history.projectId(),
            history.targetFilePath()
        );

        return new ApplyCodeChangeResponse(
            history.proposalId(),
            rollbackHistoryId,
            history.projectId(),
            history.targetFilePath(),
            history.beforeCode(),
            "ROLLBACK",
            LocalDateTime.now()
        );
    }

    private void validateProposalRequest(CodeChangeProposalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("기능변경 요청이 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.projectId())) {
            throw new IllegalArgumentException("기능변경은 projectId가 필요합니다.");
        }
        if (!StringUtils.hasText(request.targetFilePath())) {
            throw new IllegalArgumentException("기능변경 대상 파일 경로가 필요합니다.");
        }
        if (!StringUtils.hasText(request.originalCode())) {
            throw new IllegalArgumentException("기능변경 대상 소스코드가 전달되지 않았습니다. Workspace에서 파일을 선택한 후 다시 요청해 주세요.");
        }
        if (request.originalCode().length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("기능변경 대상 소스코드가 너무 큽니다. 현재는 300KB 이하 파일만 지원합니다.");
        }
        if (!StringUtils.hasText(request.instruction())) {
            throw new IllegalArgumentException("기능변경 요청 내용이 필요합니다.");
        }
    }

    /**
     * FastAPI/LLM을 호출해서 변경된 전체 파일 코드를 생성합니다.
     * 이전 placeholder처럼 요청 내용을 주석으로 붙이지 않습니다.
     */
    private String generateProposedCodeByLlm(CodeChangeProposalRequest request) {
        String prompt = buildModificationPrompt(request);

        AiChatRequest aiRequest = new AiChatRequest(
            "PROJECT_CHAT",
            "MODIFY_EXISTING_CODE",
            request.projectId(),
            request.sessionId(),
            prompt,
            List.of(
                Map.of("type", "workMode", "value", "MODIFY_EXISTING_CODE"),
                Map.of("type", "targetFilePath", "value", request.targetFilePath()),
                Map.of("type", "sourceOrigin", "value", "WORKSPACE_ORIGINAL_CODE")
            ),
            Map.of(
                "targetFilePath", request.targetFilePath(),
                "originalCode", request.originalCode()
            )
        );

        log.debug("기능변경 LLM 요청 시작. projectId={}, targetFilePath={}, originalLength={}",
            request.projectId(),
            request.targetFilePath(),
            request.originalCode().length()
        );

        AiChatResponse aiResponse = fastApiAiClient.ragQuery(aiRequest);
        String proposedCode = extractProposedCode(aiResponse);

        if (!StringUtils.hasText(proposedCode)) {
            throw new IllegalStateException("LLM이 변경된 소스코드를 반환하지 않았습니다.");
        }
        if (proposedCode.equals(request.originalCode())) {
            log.warn("LLM 변경 결과가 원본과 동일합니다. projectId={}, targetFilePath={}",
                request.projectId(),
                request.targetFilePath()
            );
        }

        return proposedCode;
    }

    private String buildModificationPrompt(CodeChangeProposalRequest request) {
        return """
            당신은 기존 소스코드를 수정하는 전문 개발 도우미입니다.

            반드시 지켜야 할 규칙:
            1. 사용자의 변경 요구사항을 실제 코드에 반영하세요.
            2. 요청 내용을 주석으로만 추가하지 마세요.
            3. 변경된 전체 파일 코드를 반환하세요.
            4. 설명은 반환하지 말고, 코드만 반환하세요.
            5. 가능하면 하나의 코드블록 안에 전체 코드를 넣어 반환하세요.
            6. 기존 기능, import, export, package 선언을 유지하세요.
            7. 대상 파일의 언어와 스타일을 유지하세요.

            대상 파일:
            %s

            사용자 변경 요구사항:
            %s

            현재 Workspace에 열린 기존 전체 소스코드:
            ```
            %s
            ```
            """.formatted(
                request.targetFilePath(),
                request.instruction(),
                request.originalCode()
            );
    }

    private String extractProposedCode(AiChatResponse response) {
        String answer = response == null ? null : response.answer();
        if (!StringUtils.hasText(answer)) {
            throw new IllegalStateException("FastAPI/LLM 응답이 비어 있습니다.");
        }

        String text = answer.trim();
        String fenced = extractFirstCodeFence(text);
        if (StringUtils.hasText(fenced)) {
            return fenced.stripTrailing();
        }

        // LLM이 코드블록 없이 전체 코드를 그대로 반환한 경우 허용합니다.
        return text.stripTrailing();
    }

    private String extractFirstCodeFence(String text) {
        int firstFence = text.indexOf("```");
        if (firstFence < 0) {
            return null;
        }

        int contentStart = text.indexOf('\n', firstFence + 3);
        if (contentStart < 0) {
            return null;
        }
        contentStart += 1;

        int secondFence = text.indexOf("```", contentStart);
        if (secondFence < 0) {
            return null;
        }

        return text.substring(contentStart, secondFence);
    }

    private ProposalState getProposalState(String proposalId) {
        ProposalState proposal = proposals.get(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("기능변경 제안을 찾을 수 없습니다: " + proposalId);
        }
        return proposal;
    }

    private void trimHistories(String projectId, String targetFilePath) {
        List<HistoryState> ordered = histories.values().stream()
            .filter(h -> equals(projectId, h.projectId()))
            .filter(h -> equals(targetFilePath, h.targetFilePath()))
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .toList();

        for (int i = MAX_HISTORY_PER_FILE; i < ordered.size(); i++) {
            histories.remove(ordered.get(i).historyId());
        }
    }

    private boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private record ProposalState(
        String proposalId,
        String projectId,
        String sessionId,
        String targetFilePath,
        String instruction,
        String originalCode,
        String proposedCode,
        String diffText,
        String originalHash,
        String proposedHash,
        String status,
        LocalDateTime createdAt
    ) {
        CodeChangeProposalResponse toResponse() {
            return new CodeChangeProposalResponse(
                proposalId,
                projectId,
                sessionId,
                targetFilePath,
                instruction,
                originalCode,
                proposedCode,
                diffText,
                status,
                createdAt
            );
        }

        ProposalState withStatus(String nextStatus) {
            return new ProposalState(
                proposalId,
                projectId,
                sessionId,
                targetFilePath,
                instruction,
                originalCode,
                proposedCode,
                diffText,
                originalHash,
                proposedHash,
                nextStatus,
                createdAt
            );
        }
    }

    private record HistoryState(
        String historyId,
        String projectId,
        String proposalId,
        String targetFilePath,
        String instruction,
        String beforeCode,
        String afterCode,
        String actionType,
        LocalDateTime createdAt
    ) {
        CodeChangeHistoryResponse toResponse() {
            return new CodeChangeHistoryResponse(
                historyId,
                projectId,
                proposalId,
                targetFilePath,
                instruction,
                beforeCode,
                afterCode,
                actionType,
                createdAt
            );
        }
    }
}
