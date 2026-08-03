package com.booking.service.dto.response;

import java.util.List;

public record BookingHistoryPageResponse (
        Long bookingId,
        long totalCount,
        List<BookingHistoryResponse> items
) {
}
