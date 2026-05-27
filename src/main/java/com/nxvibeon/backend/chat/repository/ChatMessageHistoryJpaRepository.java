package com.nxvibeon.backend.chat.repository;

import com.nxvibeon.backend.chat.domain.ChatMessageHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatMessageHistoryJpaRepository extends JpaRepository<ChatMessageHistoryEntity, String> {
    List<ChatMessageHistoryEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessageHistoryEntity> findByProjectIdAndSessionIdOrderByCreatedAtAsc(String projectId, String sessionId);

    List<ChatMessageHistoryEntity> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    List<ChatMessageHistoryEntity> findByProjectIdIsNullOrderByCreatedAtDesc(Pageable pageable);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ChatMessageHistoryEntity h where h.projectId = :projectId")
    long deleteByProjectIdValue(@Param("projectId") String projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ChatMessageHistoryEntity h
         where h.sessionId = :sessionId
           and ((:projectId is null and h.projectId is null) or h.projectId = :projectId)
        """)
    long deleteByProjectIdAndSessionIdNullable(
        @Param("projectId") String projectId,
        @Param("sessionId") String sessionId
    );

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         where h.projectId = :projectId
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> findProjectSessionSummaries(
        @Param("projectId") String projectId,
        Pageable pageable
    );

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         where h.projectId is null
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> findGeneralSessionSummaries(Pageable pageable);

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> findRecentSessionSummaries(Pageable pageable);

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         where lower(h.content) like lower(concat('%', :keyword, '%'))
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> searchSessionSummaries(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         where h.projectId = :projectId
           and lower(h.content) like lower(concat('%', :keyword, '%'))
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> searchProjectSessionSummaries(
        @Param("projectId") String projectId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        select h.projectId as projectId,
               h.sessionId as sessionId,
               count(h.id) as messageCount,
               max(h.createdAt) as lastMessageAt
          from ChatMessageHistoryEntity h
         where h.projectId is null
           and lower(h.content) like lower(concat('%', :keyword, '%'))
         group by h.projectId, h.sessionId
         order by max(h.createdAt) desc
        """)
    List<ChatSessionSummaryProjection> searchGeneralSessionSummaries(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    interface ChatSessionSummaryProjection {
        String getProjectId();
        String getSessionId();
        long getMessageCount();
        OffsetDateTime getLastMessageAt();
    }
}
