package com.booking.service.service;

import com.booking.service.AbstractIntegrationTest;
import com.booking.service.entity.Booking;
import com.booking.service.entity.BookingStatus;
import com.booking.service.entity.ProcessedEventType;
import com.booking.service.repository.BookingRepository;
import com.booking.service.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ProcessedEventServiceTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProcessedEventService processedEventService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldSaveProcessedEvent() {
        UUID key = UUID.randomUUID();

        processedEventService.save(
                key,
                ProcessedEventType.BOOKING_JOB_CONFIRMED,
                1L
        );

        assertTrue(
                processedEventService.isProcessed(
                        ProcessedEventType.BOOKING_JOB_CONFIRMED,
                        key
                )
        );
    }

    @Test
    void shouldTreatDifferentEventTypesAsDifferentEvents() {
        UUID key = UUID.randomUUID();

        processedEventService.save(
                key,
                ProcessedEventType.BOOKING_JOB_CONFIRMED,
                1L
        );

        assertTrue(
                processedEventService.isProcessed(
                        ProcessedEventType.BOOKING_JOB_CONFIRMED,
                        key
                )
        );

        assertFalse(
                processedEventService.isProcessed(
                        ProcessedEventType.BOOKING_JOB_DENIED,
                        key
                )
        );
    }

    @Test
    void shouldProcessDuplicateEventOnlyOnce() {

        OffsetDateTime now = OffsetDateTime.now();

        Booking booking = Booking.create(
                1L,
                100L,
                now.toLocalDate().plusDays(1),
                now.toLocalDate().plusDays(2),
                now
        );

        booking.setCatalogRequestId(UUID.randomUUID());
        bookingRepository.save(booking);

        UUID eventId = UUID.randomUUID();

        bookingService.handleBookingJobConfirmed(
                booking.getCatalogRequestId(),
                eventId
        );

        bookingService.handleBookingJobConfirmed(
                booking.getCatalogRequestId(),
                eventId
        );

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();

        assertEquals(
                BookingStatus.CONFIRMED,
                updated.getStatus()
        );

        assertEquals(
                1,
                processedEventRepository.countByEventTypeAndMessageKey(
                        ProcessedEventType.BOOKING_JOB_CONFIRMED,
                        eventId
                )
        );
    }
}
