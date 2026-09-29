package com.safari.wildlife.service.observer;

import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.Payment;
import com.safari.wildlife.service.AuditService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Concrete Observer: records every booking/finance event in the audit log for the Admin. */
@Component
@Order(3)
public class AuditLogObserver implements BookingEventObserver {

    private static final String IP = "127.0.0.1";

    private final AuditService auditService;

    public AuditLogObserver(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void update(BookingEvent event) {
        Booking booking = event.getBooking();
        Payment payment = event.getPayment();
        String ref = booking.getBookingReference();

        switch (event.getType()) {
            case BOOKING_CREATED -> log(event, "BOOKING_CREATE", "Booking", ref,
                    "Created booking for " + booking.getTotalGuests() + " guests, total: $" + booking.getTotalAmount()
                            + (event.getDetail() != null ? " (" + event.getDetail() + ")" : ""));
            case BOOKING_APPROVED -> log(event, "BOOKING_APPROVE", "Booking", ref,
                    "Approved booking for customer: " + event.getCustomerUser().getEmail());
            case BOOKING_REJECTED -> log(event, "BOOKING_REJECT", "Booking", ref,
                    "Rejected booking. Reason: " + event.getDetail());
            case PAYMENT_CONFIRMED -> log(event, "PAYMENT_SUCCESS", "Payment", payment.getTransactionRef(),
                    "Paid $" + payment.getAmount() + " (" + payment.getPaymentMethod() + ") for booking " + ref);
            case PAYMENT_FAILED -> log(event, "PAYMENT_FAILED", "Payment", payment.getTransactionRef(),
                    payment.getPaymentMethod() + " payment failed for booking " + ref);
            case BANK_TRANSFER_SUBMITTED -> log(event, "BANK_TRANSFER_SUBMITTED", "Payment", payment.getTransactionRef(),
                    "Bank transfer submitted for booking " + ref);
            case BANK_TRANSFER_VERIFIED -> log(event, "BANK_TRANSFER_VERIFIED", "Payment", payment.getTransactionRef(),
                    "Verified bank transfer for booking " + ref);
            case BANK_TRANSFER_REJECTED -> log(event, "BANK_TRANSFER_REJECTED", "Payment", payment.getTransactionRef(),
                    "Rejected bank transfer for booking " + ref);
            case REFUND_PROCESSED -> log(event, "REFUND_PROCESS", "Refund", event.getRefund().getRefundRef(),
                    "Processed refund of $" + event.getRefund().getAmount() + " for payment ID: " + payment.getId());
            default -> { /* BOOKING_UPDATED was not audited before either */ }
        }
    }

    private void log(BookingEvent event, String action, String entity, String entityId, String details) {
        auditService.logAction(event.getActor(), action, entity, entityId, details, IP);
    }
}
