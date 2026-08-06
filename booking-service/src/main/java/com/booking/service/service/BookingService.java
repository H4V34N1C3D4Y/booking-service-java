package com.booking.service.service;

import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.dto.response.BookingStatsResponse;
import com.booking.service.dto.response.ResourceStats;
import com.booking.service.entity.Booking;
import com.booking.service.entity.BookingHistoryReason;
import com.booking.service.entity.BookingStatus;
import com.booking.service.entity.ProcessedEventType;
import com.booking.service.exception.BusinessException;
import com.booking.service.messaging.contracts.CancelBookingJobByRequestIdRequest;
import com.booking.service.messaging.contracts.CreateBookingJobRequest;
import com.booking.service.messaging.listener.BookingEventPublisher;
import com.booking.service.notification.NotificationClient;
import com.booking.service.notification.contracts.BookingNotificationEvent;
import com.booking.service.notification.contracts.NotificationRequest;
import com.booking.service.notification.contracts.StatisticsCacheEvictEvent;
import com.booking.service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.booking.service.messaging.contracts.BookingStatusChangedEvent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис для работы с бронированиями
 * Объединяет CRUD операции, бизнес-логику и обработку событий
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;
    private final CurrentDateTimeProvider dateTimeProvider;
    private final BookingHistoryService bookingHistoryService;
    private final ProcessedEventService processedEventService;
    private final OutboxService outboxService;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final StatisticsCacheService statisticsCacheService;

    // === КОМАНДЫ (Use Cases) ===

    /**
     * Создать новое бронирование
     * Отправляет асинхронную команду в Catalog Service для создания booking job
     *
     * @return ID созданного бронирования
     */
    @Transactional
    public Long createBooking(Long userId, Long resourceId, LocalDate bookedFrom, LocalDate bookedTo) {
        Booking booking = Booking.create(userId, resourceId, bookedFrom, bookedTo, dateTimeProvider.utcNow());

        UUID requestId = UUID.randomUUID();
        booking.setCatalogRequestId(requestId);

        saveBookingHistoryAndOutbox(
                booking,
                null,
                BookingHistoryReason.BOOKING_CREATED,
                "System"
        );

        applicationEventPublisher.publishEvent(
                new StatisticsCacheEvictEvent()
        );

        CreateBookingJobRequest command = new CreateBookingJobRequest(
                UUID.randomUUID(),
                requestId,
                booking.getResourceId(),
                booking.getBookedFrom(),
                booking.getBookedTo()
        );

        bookingEventPublisher.publishCreateBookingJob(command);

        log.info("Создано бронирование с ID: {} и requestId: {}", booking.getId(), requestId);
        return booking.getId();
    }

    /**
     * Отменить бронирование
     * Отправляет асинхронную команду в Catalog Service для отмены booking job
     *
     * @param id идентификатор бронирования
     */
    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Бронирование с указанным id: '" + id + "' не найдено."));

        BookingStatus previousStatus = booking.getStatus();
        OffsetDateTime now = dateTimeProvider.utcNow();
        booking.startCancellation(now);

        saveBookingHistoryAndOutbox(
                booking,
                previousStatus,
                BookingHistoryReason.USER_CANCELLATION_REQUEST,
                "System"
        );

        applicationEventPublisher.publishEvent(
                new StatisticsCacheEvictEvent()
        );


        applicationEventPublisher.publishEvent(
                new BookingNotificationEvent(
                        NotificationRequest.from(
                                booking,
                                now,
                                "Запрос на отмену принят"
                        )
                )
        );


        if (booking.getCatalogRequestId() != null) {
            CancelBookingJobByRequestIdRequest command = new CancelBookingJobByRequestIdRequest(
                    UUID.randomUUID(),
                    booking.getCatalogRequestId()
            );

            bookingEventPublisher.publishCancelBookingJob(command);
        }


        log.info("Инициирована отмена бронирования, id=: {}", id);
    }

    // === ЗАПРОСЫ (Queries) ===

    /**
     * Получить бронирование по ID
     *
     * @param id идентификатор бронирования
     * @return бронирование
     */
    @Transactional(readOnly = true)
    public Booking getById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Бронирование с указанным id: '" + id + "' не найдено."));
    }

    /**
     * Получить бронирования по фильтрам с пагинацией
     *
     * @param userId идентификатор пользователя (опционально)
     * @param resourceId идентификатор ресурса (опционально)
     * @param status статус бронирования (опционально)
     * @param pageNumber номер страницы
     * @param pageSize размер страницы
     * @return страница с бронированиями
     */
    @Transactional(readOnly = true)
    public List<Booking> getByFilter(Long userId, Long resourceId, BookingStatus status,
                                     int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return bookingRepository.findByFilter(userId, resourceId, status, pageable);
    }

    /**
     * Получить только статус бронирования по ID
     *
     * @param id идентификатор бронирования
     * @return статус бронирования или null
     */
    @Transactional(readOnly = true)
    public BookingStatus getStatusById(Long id) {
        return bookingRepository.findStatusById(id);
    }

    // === EVENT HANDLERS (Обработка асинхронных событий от Catalog Service) ===

    /**
     * Обработать событие подтверждения booking job от Catalog Service
     * Обновляет статус бронирования на CONFIRMED
     *
     * @param requestId идентификатор запроса
     */
    @Transactional
    public void handleBookingJobConfirmed(UUID requestId, UUID eventId) {
        log.info("Получено событие BookingJobConfirmed: requestId={}", requestId);

        if (processedEventService.isProcessed(ProcessedEventType.BOOKING_JOB_CONFIRMED, eventId)) {
            log.warn("Дублирующее событие подтверждения проигнорировано: eventId={}", eventId);
            return;
        }

        Booking booking = bookingRepository.findByCatalogRequestId(requestId).orElse(null);
        if (booking == null) {
            log.warn("Бронирование не найдено по requestId: {}. Событие проигнорировано.", requestId);
            return;
        }

        log.info("Найдено бронирование: id={}, статус={}. Подтверждаем...",
                booking.getId(), booking.getStatus());


        if (booking.getStatus() == BookingStatus.CANCELLATION_PENDING) {
            log.warn(
                    "Зафиксировано состояние гонки. Отмена бронирования id={} отложена, Catalog подтвердил бронирование.",
                    booking.getId()
            );
        }

        BookingStatus previousStatus = booking.getStatus();
        BookingHistoryReason reason =
                previousStatus == BookingStatus.CANCELLATION_PENDING
                        ? BookingHistoryReason.RACE_CONDITION
                        : BookingHistoryReason.BOOKING_CONFIRMED;

        booking.confirm();

        saveBookingHistoryAndOutbox(
                booking,
                previousStatus,
                reason,
                "System"
        );

        applicationEventPublisher.publishEvent(
                new StatisticsCacheEvictEvent()
        );

        applicationEventPublisher.publishEvent(
                new BookingNotificationEvent(
                        NotificationRequest.from(
                                booking,
                                dateTimeProvider.utcNow(),
                                "Бронирование подтверждено"
                        )
                )
        );

        processedEventService.save(
                eventId,
                ProcessedEventType.BOOKING_JOB_CONFIRMED,
                booking.getId()
        );

        log.info("Бронирование успешно подтверждено: id={}, новый статус={}",
                booking.getId(), booking.getStatus());
    }

    /**
     * Обработать событие отклонения booking job от Catalog Service
     * Отменяет бронирование
     *
     * @param requestId идентификатор запроса
     */
    @Transactional
    public void handleBookingJobDenied(UUID requestId, UUID eventId) {
        log.info("Получено событие BookingJobDenied: requestId={}", requestId);

        if (processedEventService.isProcessed(ProcessedEventType.BOOKING_JOB_DENIED, eventId)) {
            log.warn("Дублирующее событие отмены проигнорировано: eventId={}", eventId);
            return;
        }

        Booking booking = bookingRepository.findByCatalogRequestId(requestId).orElse(null);
        if (booking == null) {
            log.warn("Бронирование не найдено по requestId: {}. Событие проигнорировано.", requestId);
            return;
        }


        log.info("Найдено бронирование: id={}, статус={}. Отменяем...",
                booking.getId(), booking.getStatus());

        BookingStatus previousStatus = booking.getStatus();
        OffsetDateTime now  = dateTimeProvider.utcNow();
        booking.cancel(now.toLocalDate());

        saveBookingHistoryAndOutbox(
                booking,
                previousStatus,
                BookingHistoryReason.BOOKING_DENIED,
                "System"
        );

        applicationEventPublisher.publishEvent(
                new BookingNotificationEvent(
                        NotificationRequest.from(
                                booking,
                                now,
                                "Бронирование отклонено"
                        )
                )
        );

        processedEventService.save(
                eventId,
                ProcessedEventType.BOOKING_JOB_DENIED,
                booking.getId()
        );

        log.info("Бронирование успешно отменено: id={}, новый статус={}",
                booking.getId(), booking.getStatus());
    }

    /**
     * Обработать событие ошибки от Catalog Service
     *
     * @param requestId идентификатор запроса
     */
    @Transactional
    public void handleError(UUID requestId, UUID eventId) {
        log.info("Получено событие ошибки из DLQ: requestId={}", requestId);

        if (processedEventService.isProcessed(ProcessedEventType.CANCEL_BOOKING_ERROR, eventId)) {
            log.warn("Дублирующее событие ошибки проигнорировано: eventId={}", eventId);
            return;
        }

        Booking booking = bookingRepository
                .findByCatalogRequestId(requestId)
                .orElse(null);

        if (booking == null) {
            log.warn("Бронирование не найдено по requestId: {}. Событие проигнорировано.", requestId);
            return;
        }

        if (booking.getStatus() != BookingStatus.CANCELLATION_PENDING) {
            log.warn(
                    "Повторная ошибка отмены проигнорирована: requestId={}, status={}",
                    requestId,
                    booking.getStatus()
            );
            return;
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.rollbackCancellation();

        saveBookingHistoryAndOutbox(
                booking,
                previousStatus,
                BookingHistoryReason.ROLLBACK,
                "System"
        );

        applicationEventPublisher.publishEvent(
                new StatisticsCacheEvictEvent()
        );

        applicationEventPublisher.publishEvent(
                new BookingNotificationEvent(
                        NotificationRequest.from(
                                booking,
                                dateTimeProvider.utcNow(),
                                "Отмена бронирования не выполнена"
                        )
                )
        );

        processedEventService.save(
                eventId,
                ProcessedEventType.CANCEL_BOOKING_ERROR,
                booking.getId()
        );

        log.info("Произошёл успешный откат события: requestId={}, status={}", requestId, booking.getStatus().getValue());
    }

    private void saveBookingHistoryAndOutbox(
            Booking booking,
            BookingStatus previousStatus,
            BookingHistoryReason reason,
            String initiator
    ) {
        bookingRepository.save(booking);

        OffsetDateTime now = dateTimeProvider.utcNow();

        bookingHistoryService.saveHistory(
                booking.getId(),
                previousStatus,
                booking.getStatus(),
                reason,
                initiator,
                now
        );

        outboxService.saveBookingStatusChanged(BookingStatusChangedEvent.create(
                booking.getId(),
                previousStatus,
                booking.getStatus(),
                now,
                reason
        ));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "statistics",
            key = "#dateFrom + ':' + #dateTo"
    )
    public BookingStatsResponse getStatistics(
            LocalDate dateFrom,
            LocalDate dateTo
    ) {

        OffsetDateTime from = dateFrom
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        OffsetDateTime to = dateTo
                .plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        long totalBookings =
                bookingRepository.countByCreatedAtInRange(from, to);

        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);

        byStatus.put(BookingStatus.AWAIT_CONFIRMATION, 0L);
        byStatus.put(BookingStatus.CONFIRMED, 0L);
        byStatus.put(BookingStatus.CANCELLATION_PENDING, 0L);
        byStatus.put(BookingStatus.CANCELLED, 0L);

        bookingRepository.countByStatus(from, to)
                .forEach(row -> byStatus.put(
                        (BookingStatus) row[0],
                        (Long) row[1]
                ));

        List<ResourceStats> topResources =
                bookingRepository.countTopResources(
                                from,
                                to,
                                PageRequest.of(0, 5)
                        )
                        .stream()
                        .map(row -> new ResourceStats(
                                (Long) row[0],
                                (Long) row[1]
                        ))
                        .toList();

        return new BookingStatsResponse(
                totalBookings,
                byStatus,
                topResources
        );
    }

    public void validateDateRange(
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateTo.isBefore(dateFrom)) {
            throw new BusinessException(
                    "Дата окончания периода не может быть раньше даты начала"
            );
        }
    }
}

