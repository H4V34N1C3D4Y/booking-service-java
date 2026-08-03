package com.booking.service.messaging.contracts;

import com.booking.service.entity.BookingHistoryReason;
import com.booking.service.entity.BookingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusChangedEvent {

    @JsonProperty("EventId")
    private UUID eventId;

    @JsonProperty("BookingId")
    private Long bookingId;

    @JsonProperty("PreviousStatus")
    private BookingStatus previousStatus;
    @JsonProperty("NewStatus")
    private BookingStatus newStatus;
    @JsonProperty("ChangedAt")
    private OffsetDateTime changedAt;
    @JsonProperty("Reason")
    private String reason;

    public static BookingStatusChangedEvent create(
            Long bookingId,
            BookingStatus previousStatus,
            BookingStatus newStatus,
            OffsetDateTime changedAt,
            BookingHistoryReason reason
    ) {
        return new BookingStatusChangedEvent(
                UUID.randomUUID(),
                bookingId,
                previousStatus,
                newStatus,
                changedAt,
                reason.name()
        );
    }
}
