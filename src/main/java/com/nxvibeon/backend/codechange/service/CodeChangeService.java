package com.nxvibeon.backend.codechange.service;

import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.ai.dto.AiChatRequest;
import com.nxvibeon.backend.ai.dto.AiChatResponse;
import com.nxvibeon.backend.codechange.domain.CodeChangeHistoryEntity;
import com.nxvibeon.backend.codechange.domain.CodeChangeProposalEntity;
import com.nxvibeon.backend.codechange.dto.CodeChangeApplyResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeHistoryResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalCreateRequest;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeRollbackResponse;
import com.nxvibeon.backend.codechange.repository.CodeChangeHistoryJpaRepository;
import com.nxvibeon.backend.codechange.repository.CodeChangeProposalJpaRepository;
import com.nxvibeon.backend.project.domain.ProjectEntity;
import com.nxvibeon.backend.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class CodeChangeService {
    private static final Logger log = LoggerFactory.getLogger(CodeChangeService.class);

    private static final int MAX_WORKSPACE_SOURCE_LENGTH = 300_000;

    private final ProjectService projectService;
    private final FastApiAiClient aiClient;
    private final CodeChangeProposalJpaRepository proposalRepository;
    private final CodeChangeHistoryJpaRepository historyRepository;

    public CodeChangeService(
        ProjectService projectService,
        FastApiAiClient aiClient,
        CodeChangeProposalJpaRepository proposalRepository,
        CodeChangeHistoryJpaRepository historyRepository
    ) {
        this.projectService = projectService;
        this.aiClient = aiClient;
        this.proposalRepository = proposalRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Workspace 기능변경 제안 생성.
     *
     * 중요:
     * - 이 메서드는 Backend 서버 파일 시스템에서 targetFilePath를 읽지 않습니다.
     * - Web UI Workspace 창에 현재 열려 있는 소스코드를 request.originalCode로 받아서 LLM에 전달합니다.
     * - targetFilePath는 표시, 이력, diff 제목 용도로만 사용합니다.
     */
    @Transactional
    public CodeChangeProposalResponse createProposal(CodeChangeProposalCreateRequest request) {
        ProjectEntity project = projectService.findActiveEntity(request.projectId());
        validateTargetFilePath(request.targetFilePath());
        validateWorkspaceSource(request.originalCode());
        validateInstruction(request.instruction());

        String originalCode = request.originalCode();
        String originalHash = sha256(originalCode);

        String instructionPrompt = buildModificationPrompt(
            project,
            request.targetFilePath(),
            originalCode,
            request.instruction()
        );

        log.info("Workspace 기능변경 LLM 요청 시작. projectId={}, file={}", project.getId(), request.targetFilePath());

        AiChatResponse aiResponse = aiClient.ragQuery(new AiChatRequest(
            "PROJECT_CHAT",
            "MODIFY_EXISTING_CODE",
            project.getId(),
            request.sessionId(),
            instructionPrompt,
            List.of(
                Map.of("type", "workMode", "value", "MODIFY_EXISTING_CODE"),
                Map.of("type", "targetFilePath", "value", request.targetFilePath()),
                Map.of("type", "sourceOrigin", "value", "WEB_UI_WORKSPACE_ORIGINAL_CODE")
            ),
            Map.of(
                "targetFilePath", request.targetFilePath(),
                "originalCode", originalCode
            )
        ));

        String proposedCode = extractProposedCode(aiResponse, originalCode);
        validateProposedCode(originalCode, proposedCode);

        String proposedHash = sha256(proposedCode);
        String diffText = createSimpleUnifiedDiff(originalCode, proposedCode, request.targetFilePath());

        CodeChangeProposalEntity proposal = proposalRepository.save(new CodeChangeProposalEntity(
            project.getId(),
            request.sessionId(),
            request.targetFilePath(),
            request.instruction(),
            originalCode,
            proposedCode,
            diffText,
            originalHash,
            proposedHash
        ));

        log.info(
            "Workspace 기능변경 제안 생성 완료. projectId={}, file={}, proposalId={}, changed={}",
            project.getId(),
            request.targetFilePath(),
            proposal.getId(),
            !originalHash.equals(proposedHash)
        );

        return CodeChangeProposalResponse.from(proposal);
    }

    @Transactional(readOnly = true)
    public CodeChangeProposalResponse getProposal(String proposalId) {
        return CodeChangeProposalResponse.from(findProposal(proposalId));
    }

    /**
     * 변경안 적용.
     *
     * 현재 구조에서는 실제 파일 저장을 Backend가 수행하지 않습니다.
     * Web UI가 이 응답 또는 이미 보유 중인 proposal.proposedCode를 Workspace 에디터에 반영하고,
     * 실제 저장은 Workspace 저장 버튼에서 처리합니다.
     */
    @Transactional
    public CodeChangeApplyResponse applyProposal(String proposalId) {
        CodeChangeProposalEntity proposal = findProposal(proposalId);
        if (!"PENDING".equals(proposal.getStatus())) {
            throw new IllegalArgumentException("적용 가능한 상태가 아닙니다. proposalId=" + proposalId + ", status=" + proposal.getStatus());
        }

        proposal.markApplied();

        CodeChangeHistoryEntity history = historyRepository.save(new CodeChangeHistoryEntity(
            proposal.getProjectId(),
            proposal.getId(),
            proposal.getTargetFilePath(),
            proposal.getInstruction(),
            proposal.getOriginalCode(),
            proposal.getProposedCode(),
            proposal.getOriginalHash(),
            proposal.getProposedHash(),
            "APPLY"
        ));

        pruneHistoriesToLatestThree(proposal.getProjectId(), proposal.getTargetFilePath());

        log.info(
            "Workspace 기능변경 제안 적용 처리 완료. proposalId={}, historyId={}, file={}",
            proposalId,
            history.getId(),
            proposal.getTargetFilePath()
        );

        return new CodeChangeApplyResponse(
            proposalId,
            history.getId(),
            proposal.getProjectId(),
            proposal.getTargetFilePath(),
            "APPLIED",
            "소스 반영이 승인되었습니다. Web UI Workspace에 변경 코드를 반영하세요."
        );
    }

    @Transactional(readOnly = true)
    public List<CodeChangeHistoryResponse> listHistories(String projectId, String targetFilePath) {
        projectService.validateProjectExistsIfPresent(projectId);
        return historyRepository.findTop3ByProjectIdAndTargetFilePathOrderByCreatedAtDesc(projectId, targetFilePath)
            .stream()
            .map(CodeChangeHistoryResponse::from)
            .toList();
    }

    /**
     * 원복 처리.
     *
     * 이 메서드도 Backend 파일 시스템을 직접 수정하지 않습니다.
     * Web UI는 이력의 beforeCode를 사용해서 Workspace 에디터 내용을 되돌려야 합니다.
     */
    @Transactional
    public CodeChangeRollbackResponse rollback(String historyId) {
        CodeChangeHistoryEntity sourceHistory = historyRepository.findById(historyId)
            .orElseThrow(() -> new IllegalArgumentException("변경 이력을 찾을 수 없습니다: " + historyId));

        CodeChangeHistoryEntity rollbackHistory = historyRepository.save(new CodeChangeHistoryEntity(
            sourceHistory.getProjectId(),
            sourceHistory.getProposalId(),
            sourceHistory.getTargetFilePath(),
            "원복: " + sourceHistory.getInstruction(),
            sourceHistory.getAfterCode(),
            sourceHistory.getBeforeCode(),
            sourceHistory.getAfterHash(),
            sourceHistory.getBeforeHash(),
            "ROLLBACK"
        ));

        pruneHistoriesToLatestThree(sourceHistory.getProjectId(), sourceHistory.getTargetFilePath());

        log.info(
            "Workspace 기능변경 원복 이력 생성 완료. sourceHistoryId={}, rollbackHistoryId={}, file={}",
            historyId,
            rollbackHistory.getId(),
            sourceHistory.getTargetFilePath()
        );

        return new CodeChangeRollbackResponse(
            rollbackHistory.getId(),
            historyId,
            sourceHistory.getProjectId(),
            sourceHistory.getTargetFilePath(),
            "ROLLED_BACK",
            "원복이 승인되었습니다. Web UI Workspace에 이전 코드를 반영하세요."
        );
    }

    private CodeChangeProposalEntity findProposal(String proposalId) {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new IllegalArgumentException("기능변경 제안을 찾을 수 없습니다: " + proposalId));
    }

    private void validateTargetFilePath(String targetFilePath) {
        if (!StringUtils.hasText(targetFilePath)) {
            throw new IllegalArgumentException("대상 파일 경로는 필수입니다.");
        }
        String normalized = targetFilePath.replace('\\', '/');
        if (normalized.contains("../") || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("targetFilePath는 Workspace 기준 상대경로만 허용됩니다: " + targetFilePath);
        }
    }

    private void validateWorkspaceSource(String originalCode) {
        if (!StringUtils.hasText(originalCode)) {
            throw new IllegalArgumentException(
                "기능변경 대상 소스코드가 전달되지 않았습니다. Workspace에서 파일을 선택한 후 다시 요청해 주세요."
            );
        }
        if (originalCode.length() > MAX_WORKSPACE_SOURCE_LENGTH) {
            throw new IllegalArgumentException(
                "기능변경 대상 소스코드가 너무 큽니다. 현재는 300KB 이하 파일만 지원합니다."
            );
        }
    }

    private void validateInstruction(String instruction) {
        if (!StringUtils.hasText(instruction)) {
            throw new IllegalArgumentException("기능변경 요청 내용은 필수입니다.");
        }
    }

    private void validateProposedCode(String originalCode, String proposedCode) {
        if (!StringUtils.hasText(proposedCode)) {
            throw new IllegalStateException("LLM이 변경된 소스코드를 반환하지 않았습니다.");
        }
        if (proposedCode.contains("NxVibeOn 기능변경 제안") || proposedCode.contains("요청:") && proposedCode.startsWith("/*")) {
            throw new IllegalStateException("LLM 변경 코드가 아니라 placeholder 주석 코드가 생성되었습니다. FastAPI/LLM 연결을 확인하세요.");
        }
        if (originalCode.equals(proposedCode)) {
            log.warn("LLM 변경 결과가 기존 코드와 동일합니다. instruction이 너무 모호하거나 LLM이 변경을 수행하지 않았을 수 있습니다.");
        }
    }

    private String buildModificationPrompt(ProjectEntity project, String targetFilePath, String originalCode, String instruction) {
        return """
            당신은 기존 소스코드를 수정하는 전문 개발자입니다.

            반드시 아래 규칙을 지키세요.
            1. 사용자의 기능변경 요구사항을 실제 코드로 반영하세요.
            2. 요청사항을 주석으로만 추가하지 마세요.
            3. 기존 전체 파일을 기준으로 수정된 전체 파일 코드를 반환하세요.
            4. 반환은 하나의 코드블록만 사용하세요.
            5. 코드블록 밖 설명은 쓰지 마세요.
            6. 기존 import, class name, public API는 필요하지 않으면 유지하세요.
            7. 컴파일 가능한 코드로 작성하세요.

            프로젝트명: %s
            대상 파일: %s

            기능변경 요구사항:
            %s

            기존 전체 소스코드:
            ```
            %s
            ```
            """.formatted(project.getName(), targetFilePath, instruction, originalCode);
    }

    private String extractProposedCode(AiChatResponse response, String fallbackOriginalCode) {
        String text = response == null ? "" : StringUtils.hasText(response.answer()) ? response.answer() : "";
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("AI가 변경 코드를 반환하지 않았습니다.");
        }

        String fenced = extractFirstCodeFence(text);
        if (StringUtils.hasText(fenced)) {
            return fenced.stripTrailing();
        }

        String trimmed = text.trim();
        if (looksLikePlainSourceCode(trimmed, fallbackOriginalCode)) {
            return trimmed;
        }

        throw new IllegalStateException("AI 응답에서 변경된 전체 파일 코드를 추출하지 못했습니다. LLM 응답 형식을 확인하세요.");
    }

    private String extractFirstCodeFence(String text) {
        int firstFence = text.indexOf("```");
        if (firstFence < 0) {
            return "";
        }

        int contentStart = text.indexOf('\n', firstFence + 3);
        if (contentStart < 0) {
            contentStart = firstFence + 3;
        } else {
            contentStart += 1;
        }

        int secondFence = text.indexOf("```", contentStart);
        if (secondFence <= contentStart) {
            return "";
        }

        return text.substring(contentStart, secondFence);
    }

    private boolean looksLikePlainSourceCode(String text, String originalCode) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (text.length() < Math.max(20, originalCode.length() / 10)) {
            return false;
        }
        return text.contains("{")
            || text.contains("import ")
            || text.contains("package ")
            || text.contains("function ")
            || text.contains("const ")
            || text.contains("class ")
            || text.contains("export ")
            || text.contains("def ");
    }

    private String createSimpleUnifiedDiff(String before, String after, String filePath) {
        if (before.equals(after)) {
            return "변경 사항이 없습니다.";
        }
        List<String> beforeLines = before.lines().toList();
        List<String> afterLines = after.lines().toList();
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(filePath).append("\n");
        diff.append("+++ ").append(filePath).append("\n");
        int max = Math.max(beforeLines.size(), afterLines.size());
        for (int i = 0; i < max; i++) {
            String oldLine = i < beforeLines.size() ? beforeLines.get(i) : null;
            String newLine = i < afterLines.size() ? afterLines.get(i) : null;
            if (oldLine == null) {
                diff.append("+").append(newLine).append("\n");
            } else if (newLine == null) {
                diff.append("-").append(oldLine).append("\n");
            } else if (!oldLine.equals(newLine)) {
                diff.append("-").append(oldLine).append("\n");
                diff.append("+").append(newLine).append("\n");
            }
        }
        return diff.toString();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시를 사용할 수 없습니다.", e);
        }
    }

    private void pruneHistoriesToLatestThree(String projectId, String targetFilePath) {
        List<CodeChangeHistoryEntity> histories = new ArrayList<>(
            historyRepository.findByProjectIdAndTargetFilePathOrderByCreatedAtDesc(projectId, targetFilePath)
        );
        if (histories.size() <= 3) {
            return;
        }
        histories.subList(3, histories.size()).forEach(historyRepository::delete);
    }
}
