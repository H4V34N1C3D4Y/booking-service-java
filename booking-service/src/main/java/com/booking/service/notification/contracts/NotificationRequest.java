package com.booking.service.notification.contracts;

import com.booking.service.entity.Booking;
import com.booking.service.entity.BookingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class NotificationRequest {

    Long bookingId;

    Long userId;

    BookingStatus status;

    OffsetDateTime changedAt;

    String message;

    public static NotificationRequest from(
            Booking booking,
            OffsetDateTime changedAt,
            String message
    ) {
        return NotificationRequest.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .status(booking.getStatus())
                .changedAt(changedAt)
                .message(message)
                .build();
    }
}