package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.dto.response.BookingHistoryResponse;
import com.booking.service.entity.BookingHistory;
import com.booking.service.entity.BookingStatus;
import com.booking.service.repository.BookingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingHistoryService {

    private final BookingHistoryRepository bookingHistoryRepository;
    private final CurrentDateTimeProvider dateTimeProvider;
    @Transactional
    public void saveHistory(
            Long bookingId,
            BookingStatus previousStatus,
            BookingStatus newStatus,
            String reason,
            String initiator
    ) {
        BookingHistory history = BookingHistory.create(
                bookingId,
                previousStatus,
                newStatus,
                dateTimeProvider.utcNow(),
                reason,
                initiator
        );

        bookingHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<BookingHistory> getHistory(
            Long bookingId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("changedAt").descending()
        );

        return bookingHistoryRepository.findByBookingId(
                bookingId,
                pageable
        );
    }
}