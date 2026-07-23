package com.booking.service.service.scheduler;

import com.booking.service.config.BookingCancellationProperties;
import com.booking.service.config.CurrentDateTimeProvider;
import com.booking.service.entity.Booking;
import com.booking.service.entity.BookingStatus;
import com.booking.service.messaging.contracts.CancelBookingJobByRequestIdRequest;
import com.booking.service.messaging.listener.BookingEventPublisher;
import com.booking.service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationRetryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;
    private final CurrentDateTimeProvider dateTimeProvider;
    private final BookingCancellationProperties cancellationProperties;

    @Scheduled(fixedDelayString = "${booking.polling.interval}")
    public void retryCancellation() {
        OffsetDateTime threshold =
                dateTimeProvider.utcNow()
                        .minus(cancellationProperties.getTimeout());

        List<Booking> bookings =
                bookingRepository.findByStatusAndCancellationRequestedAtBefore(
                        BookingStatus.CANCELLATION_PENDING,
                        threshold
                );

        if (bookings.isEmpty()) {
            log.debug("Зависшие отмены не найдены");
            return;
        }

        int sent = 0;
        int errors = 0;

        for (Booking booking : bookings) {
            try {
                if (booking.getCatalogRequestId() == null) {
                    log.warn(
                            "У бронирования с id={} отсутствует catalogRequestId. Повторная отправка отмены пропущена.",
                            booking.getId()
                    );
                    continue;
                }

                CancelBookingJobByRequestIdRequest command =
                        new CancelBookingJobByRequestIdRequest(
                                UUID.randomUUID(),
                                booking.getCatalogRequestId()
                        );

                bookingEventPublisher.publishCancelBookingJob(command);
                sent++;

                log.info("Повторно отправлена команда отмены для бронирования id={}", booking.getId());

            } catch (Exception ex) {
                errors++;
                log.error(
                        "Ошибка при повторной отправке отмены бронирования id={}",
                        booking.getId(),
                        ex
                );
            }
        }
        
        log.info(
                "Повторная отправка отмен завершена. Найдено: {}, отправлено: {}, ошибок: {}",
                bookings.size(),
                sent,
                errors
        );
    }
}
