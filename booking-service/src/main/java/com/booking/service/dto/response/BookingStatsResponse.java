package com.booking.service.dto.response;

import com.booking.service.entity.BookingStatus;

import java.util.List;
import java.util.Map;

public record BookingStatsResponse(
        long totalBookings,
        Map<BookingStatus, Long> byStatus,
        List<ResourceStats> topResources
) {
}
