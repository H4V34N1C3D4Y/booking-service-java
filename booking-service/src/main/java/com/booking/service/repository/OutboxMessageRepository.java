package com.booking.service.repository;

import com.booking.service.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Query(value = """
            SELECT *
            FROM outbox_messages
            WHERE status = :status
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findForPublishing(
            @Param("status") String status,
            @Param("limit") int limit
    );

    Optional<OutboxMessage> findByEventId(UUID eventId);
}
