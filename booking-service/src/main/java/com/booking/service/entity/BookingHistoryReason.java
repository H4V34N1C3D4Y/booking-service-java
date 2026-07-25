package com.booking.service.entity;

public enum BookingHistoryReason {
    BOOKING_CREATED,
    BOOKING_CONFIRMED,
    USER_CANCELLATION_REQUEST,
    BOOKING_DENIED,
    ROLLBACK,
    RACE_CONDITION
}
