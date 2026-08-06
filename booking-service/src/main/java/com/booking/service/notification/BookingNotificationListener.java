package com.booking.service.notification;

import com.booking.service.notification.contracts.BookingNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final NotificationClient notificationClient;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onNotification(BookingNotificationEvent event) {
        notificationClient.send(event.request());
    }
}