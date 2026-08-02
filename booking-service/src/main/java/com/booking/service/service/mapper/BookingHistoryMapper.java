package com.booking.service.service.mapper;

import com.booking.service.dto.response.BookingHistoryResponse;
import com.booking.service.entity.BookingHistory;
import org.springframework.stereotype.Component;

@Component
public class BookingHistoryMapper {
    public BookingHistoryResponse toResponse(BookingHistory history) {
        return new BookingHistoryResponse(
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangedAt(),
                history.getReason(),
                history.getInitiator()
        );
    }
}