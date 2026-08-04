package com.booking.service.service;

import com.booking.service.AbstractIntegrationTest;
import com.booking.service.entity.OutboxMessage;
import com.booking.service.repository.BookingRepository;
import com.booking.service.repository.OutboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.booking.service.entity.Booking;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RepositoryIndexesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Test
    void shouldCreateBookingsIndexes() {

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE tablename = 'bookings'
                """,
                String.class
        );

        assertThat(indexes)
                .contains(
                        "idx_bookings_created_at",
                        "idx_bookings_status_cancellation_requested_at"
                );
    }

    @Test
    void shouldCreateOutboxIndexes() {

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE tablename = 'outbox_messages'
                """,
                String.class
        );

        assertThat(indexes)
                .contains("idx_outbox_status_created_at");
    }

    @Test
    void shouldFindBookingByCatalogRequestId() {

        UUID requestId = UUID.randomUUID();

        Booking booking = Booking.create(
                1L,
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                OffsetDateTime.now()
        );

        booking.setCatalogRequestId(requestId);

        bookingRepository.saveAndFlush(booking);

        Optional<Booking> found = bookingRepository.findByCatalogRequestId(requestId);

        assertThat(found).isPresent();
        assertThat(found.get().getCatalogRequestId()).isEqualTo(requestId);
    }

    @Test
    void shouldFindOutboxMessageByEventId() {

        UUID eventId = UUID.randomUUID();

        OutboxMessage message = OutboxMessage.create(
                eventId,
                "BookingStatusChangedEvent",
                "{}",
                OffsetDateTime.now()
        );

        outboxMessageRepository.saveAndFlush(message);

        Optional<OutboxMessage> found = outboxMessageRepository.findByEventId(eventId);

        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(eventId);
    }

    @Test
    void shouldCreateUniqueIndexForCatalogRequestId() {

        String indexDefinition = jdbcTemplate.queryForObject(
                """
                SELECT indexdef
                FROM pg_indexes
                WHERE indexname = 'uk_bookings_catalog_request_id'
                """,
                String.class
        );

        assertThat(indexDefinition)
                .isNotNull()
                .containsIgnoringCase("unique");
    }
}