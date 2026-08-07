package com.booking.service.dto.response;

import com.booking.service.entity.BookingHistoryReason;
import com.booking.service.entity.BookingStatus;

import java.time.OffsetDateTime;

public record BookingHistoryResponse(
        BookingStatus previousStatus,
        BookingStatus newStatus,
        OffsetDateTime changedAt,
        BookingHistoryReason reason,
        String initiator
) {
}