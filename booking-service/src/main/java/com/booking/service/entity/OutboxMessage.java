package com.booking.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
@Getter
@NoArgsConstructor
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxMessageStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "last_error")
    private String lastError;

    public static OutboxMessage create(
            UUID eventId,
            String messageType,
            String payload,
            OffsetDateTime createdAt
    ) {
        OutboxMessage message = new OutboxMessage();

        message.eventId = eventId;
        message.messageType = messageType;
        message.payload = payload;
        message.createdAt = createdAt;
        message.status = OutboxMessageStatus.NEW;
        message.attempts = 0;

        return message;
    }

    public void markAsSent(OffsetDateTime sentAt) {
        this.status = OutboxMessageStatus.SENT;
        this.sentAt = sentAt;
        this.lastError = null;
    }

    public void registerFailure(String error) {
        this.attempts++;
        this.lastError = error;
    }

    public void markAsFailed() {
        this.status = OutboxMessageStatus.FAILED;
    }
}
