package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
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

    private final ProcessedEventRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean register(UUID eventId, String eventType, Long bookingId) {

        if (repository.existsByEventId(eventId)) {
            return false;
        }

        return saveProcessedEvent(eventId, eventType, bookingId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveProcessedEvent(UUID eventId, String eventType, Long bookingId) {
        try {
            processedEventRepository.save(
                    ProcessedEvent.create(
                            eventId,
                            dateTimeProvider.utcNow(),
                            eventType,
                            bookingId
                    )
            );
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
