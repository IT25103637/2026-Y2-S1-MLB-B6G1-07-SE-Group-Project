package com.safari.wildlife.service.observer;

import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.Payment;
import com.safari.wildlife.entity.Refund;
import com.safari.wildlife.entity.User;

import java.time.LocalDateTime;

/**
 * The message the Subject sends to every Observer: what happened, to which booking, and who did it.
 * Payment, refund and detail are only filled in for the event types that need them.
 */
public class BookingEvent {

    private final BookingEventType type;
    private final Booking booking;
    private final User actor;               // the user whose action caused the event
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private Payment payment;
    private Refund refund;
    private String detail;                  // e.g. rejection reason or the pricing rule that was applied

    public BookingEvent(BookingEventType type, Booking booking, User actor) {
        this.type = type;
        this.booking = booking;
        this.actor = actor;
    }

    public BookingEvent withPayment(Payment payment) {
        this.payment = payment;
        return this;
    }

    public BookingEvent withRefund(Refund refund) {
        this.refund = refund;
        return this;
    }

    public BookingEvent withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public BookingEventType getType() { return type; }
    public Booking getBooking() { return booking; }
    public User getActor() { return actor; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public Payment getPayment() { return payment; }
    public Refund getRefund() { return refund; }
    public String getDetail() { return detail; }

    /** The customer who owns the booking. */
    public User getCustomerUser() {
        return booking.getCustomer().getUser();
    }
}
