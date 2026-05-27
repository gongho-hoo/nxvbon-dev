package com.nxvibeon.backend.chat.service;

import com.nxvibeon.backend.chat.repository.ChatMessageHistoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ChatHistoryRetentionService {
    private static final Logger log = LoggerFactory.getLogger(ChatHistoryRetentionService.class);

    private final ChatMessageHistoryJpaRepository repository;
    private final int retentionMonths;

    public ChatHistoryRetentionService(
        ChatMessageHistoryJpaRepository repository,
        @Value("${nxvibeon.chat-history.retention-months:3}") int retentionMonths
    ) {
        this.repository = repository;
        this.retentionMonths = retentionMonths;
    }

    @Scheduled(cron = "${nxvibeon.chat-history.cleanup-cron:0 10 0 * * *}")
    @Transactional
    public void deleteExpiredHistories() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(retentionMonths);
        long deletedCount = repository.deleteByCreatedAtBefore(cutoff);
        log.info("채팅 이력 보관 기간 초과 데이터 삭제 완료. retentionMonths={}, cutoff={}, deletedCount={}", retentionMonths, cutoff, deletedCount);
    }
}
