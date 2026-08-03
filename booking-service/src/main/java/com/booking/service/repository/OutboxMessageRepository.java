package com.booking.service.repository;

import com.booking.service.entity.OutboxMessage;
import com.booking.service.entity.OutboxMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop100ByStatusOrderByCreatedAtAsc(OutboxMessageStatus status);

    Optional<OutboxMessage> findByEventId(UUID eventId);
}
