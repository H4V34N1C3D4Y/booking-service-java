package com.booking.service.repository;

import com.booking.service.entity.ProcessedEvent;
import com.booking.service.entity.ProcessedEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventTypeAndMessageKey(ProcessedEventType eventType, UUID messageKey);
}
