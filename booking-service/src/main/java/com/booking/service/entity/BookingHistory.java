package com.booking.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_history")
@Getter
@NoArgsConstructor
public class BookingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "previous_status")
    private BookingStatus previousStatus;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "new_status", nullable = false)
    private BookingStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private BookingHistoryReason reason;

    @Column(name = "initiator", nullable = false)
    private String initiator;

    public static BookingHistory create(
            Long bookingId,
            BookingStatus previousStatus,
            BookingStatus newStatus,
            OffsetDateTime changedAt,
            BookingHistoryReason reason,
            String initiator
    ) {
        BookingHistory history = new BookingHistory();
        history.bookingId = bookingId;
        history.previousStatus = previousStatus;
        history.newStatus = newStatus;
        history.changedAt = changedAt;
        history.reason = reason;
        history.initiator = initiator;
        return history;
    }
}
