package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.entity.OutboxMessage;
import com.booking.service.entity.OutboxMessageStatus;
import com.booking.service.messaging.contracts.BookingStatusChangedEvent;
import com.booking.service.messaging.listener.BookingEventPublisher;
import com.booking.service.repository.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private final OutboxMessageRepository repository;
    private final BookingEventPublisher bookingEventPublisher;
    private final ObjectMapper objectMapper;
    private final CurrentDateTimeProvider dateTimeProvider;

    @Value("${booking.outbox.max-attempts}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${booking.outbox.interval}")
    @Transactional
    public void publishMessages() {

        List<OutboxMessage> messages = repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxMessageStatus.NEW);

        if (messages.isEmpty()) {
            return;
        }

        for (OutboxMessage message : messages) {
            try {
                BookingStatusChangedEvent event =
                        objectMapper.readValue(
                                message.getPayload(),
                                BookingStatusChangedEvent.class
                        );

                bookingEventPublisher.publishBookingStatusChanged(event);

                message.markAsSent(dateTimeProvider.utcNow());

                log.info("Outbox сообщение отправлено: eventId={}", message.getEventId());

            } catch (Exception ex) {

                message.incrementAttempts(maxAttempts);

                log.warn("Не удалось отправить outbox сообщение: eventId={}, attempt={}",
                        message.getEventId(),
                        message.getAttempts(),
                        ex
                );
            }
        }
    }
}