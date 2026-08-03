package com.booking.service.job;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.entity.OutboxMessage;
import com.booking.service.entity.OutboxMessageStatus;
import com.booking.service.messaging.contracts.BookingStatusChangedEvent;
import com.booking.service.messaging.listener.BookingEventPublisher;
import com.booking.service.repository.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private final OutboxMessageRepository repository;
    private final BookingEventPublisher bookingEventPublisher;
    private final ObjectMapper objectMapper;
    private final CurrentDateTimeProvider dateTimeProvider;

    @Value("${booking.outbox.max-attempts:3}")
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

                String error = ExceptionUtils.getRootCauseMessage(ex);

                if (error.length() > 1000) {
                    error = error.substring(0, 1000);
                }

                message.registerFailure(error);

                if (message.getAttempts() >= maxAttempts) {
                    message.markAsFailed();
                }

                log.warn("Не удалось отправить outbox сообщение: eventId={}, attempt={}",
                        message.getEventId(),
                        message.getAttempts(),
                        ex
                );
            }
        }
    }
}