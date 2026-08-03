package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.entity.BookingHistory;
import com.booking.service.entity.BookingHistoryReason;
import com.booking.service.entity.BookingStatus;
import com.booking.service.exception.BusinessException;
import com.booking.service.repository.BookingHistoryRepository;
import com.booking.service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingHistoryService {

    private final BookingHistoryRepository bookingHistoryRepository;
    private final CurrentDateTimeProvider dateTimeProvider;

    private final BookingRepository bookingRepository;

    @Transactional
    public void saveHistory(
            Long bookingId,
            BookingStatus previousStatus,
            BookingStatus newStatus,
            BookingHistoryReason reason,
            String initiator,
            OffsetDateTime changedAt
    ) {
        BookingHistory history = BookingHistory.create(
                bookingId,
                previousStatus,
                newStatus,
                changedAt,
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

        validatePagination(page, size);

        if (!bookingRepository.existsById(bookingId)) {
            throw new BusinessException(
                    "Бронирование с указанным id: '" + bookingId + "' не найдено."
            );
        }

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

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BusinessException("Номер страницы не может быть отрицательным");
        }

        if (size < 1 || size > 100) {
            throw new BusinessException("Размер страницы должен быть от 1 до 100");
        }
    }
}