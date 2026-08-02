package com.booking.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false)
    private UUID messageKey;
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessedEventType eventType;

    @Column(name = "booking_id")
    private Long bookingId;

    public static ProcessedEvent create(
            UUID messageKey,
            OffsetDateTime processedAt,
            ProcessedEventType eventType,
            Long bookingId
    ) {
        ProcessedEvent event = new ProcessedEvent();
        event.messageKey = messageKey;
        event.processedAt = processedAt;
        event.eventType = eventType;
        event.bookingId = bookingId;
        return event;
    }
}
