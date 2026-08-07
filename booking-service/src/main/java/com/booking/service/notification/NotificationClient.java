package com.booking.service.notification;

import com.booking.service.notification.contracts.NotificationRequest;
import com.booking.service.notification.exceptions.NotificationClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {
    private final RestClient restClient;

    @Retryable(
            retryFor = RestClientException.class,
            noRetryFor = NotificationClientException.class,
            maxAttemptsExpression = "#{@notificationProperties.retry.maxAttempts}",
            backoff = @Backoff(
                    delayExpression = "#{@notificationProperties.retry.delay}",
                    multiplierExpression = "#{@notificationProperties.retry.multiplier}"
            )
    )
    public void send(NotificationRequest request) {

        log.info(
                "Отправка уведомления: bookingId={}, userId={}, status={}",
                request.getBookingId(),
                request.getUserId(),
                request.getStatus()
        );

        restClient.post()
                .uri("/api/notifications")
                .body(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (req, res) -> {
                            throw new NotificationClientException(
                                    "Сервис уведомлений вернул ошибку 4xx: " + res.getStatusCode()
                            );
                        }
                )
                .toBodilessEntity();

        log.info("Уведомление отправлено: bookingId={}", request.getBookingId());
    }

    @Recover
    public void recover(Exception ex, NotificationRequest request) {

        log.warn(
                "Не удалось отправить уведомление после всех попыток. bookingId={}",
                request.getBookingId(),
                ex
        );
    }
}