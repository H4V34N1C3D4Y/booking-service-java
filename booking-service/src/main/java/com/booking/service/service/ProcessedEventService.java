package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.booking.service.entity.ProcessedEvent;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;
    private final CurrentDateTimeProvider dateTimeProvider;

    public boolean register(UUID eventId) {
        try {
            processedEventRepository.save(
                    ProcessedEvent.create(
                            eventId,
                            dateTimeProvider.utcNow()
                    )
            );
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
