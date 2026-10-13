package com.safari.wildlife.service.observer;

/** Things that can happen to a booking during its life cycle (booking approval and finance). */
public enum BookingEventType {
    BOOKING_CREATED,
    BOOKING_UPDATED,
    BOOKING_APPROVED,
    BOOKING_REJECTED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    BANK_TRANSFER_SUBMITTED,
    BANK_TRANSFER_VERIFIED,
    BANK_TRANSFER_REJECTED,
    REFUND_PROCESSED,
    PAYMENT_REMOVED
}
