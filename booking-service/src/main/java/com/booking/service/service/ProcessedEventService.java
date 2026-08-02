package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.entity.ProcessedEventType;
import com.booking.service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.booking.service.entity.ProcessedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;
    private final CurrentDateTimeProvider dateTimeProvider;

    public boolean isProcessed(ProcessedEventType eventType, UUID messageKey) {
        return processedEventRepository.existsByEventTypeAndMessageKey(eventType, messageKey);
    }

    public void save(
            UUID messageKey,
            ProcessedEventType eventType,
            Long bookingId
    ) {
        processedEventRepository.save(
                ProcessedEvent.create(
                        messageKey,
                        dateTimeProvider.utcNow(),
                        eventType,
                        bookingId
                )
        );
    }
}
