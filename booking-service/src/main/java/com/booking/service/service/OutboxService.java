package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.config.RabbitMqProperties;
import com.booking.service.entity.OutboxMessage;
import com.booking.service.messaging.contracts.BookingStatusChangedEvent;
import com.booking.service.repository.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final CurrentDateTimeProvider dateTimeProvider;
    private final RabbitMqProperties rabbitMqProperties;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveBookingStatusChanged(BookingStatusChangedEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxMessage message = OutboxMessage.create(
                    event.getEventId(),
                    rabbitMqProperties.getMessageTypes().getBookingStatusChanged(),
                    payload,
                    dateTimeProvider.utcNow()
            );

            outboxMessageRepository.save(message);

            log.debug("Outbox сообщение сохранено: eventId={}", event.getEventId());

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось сериализовать BookingStatusChangedEvent", ex);
        }
    }
}