package com.safari.wildlife.service.observer;

import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.Payment;
import com.safari.wildlife.service.NotificationService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Concrete Observer: shows an in-app notification on the customer's dashboard. */
@Component
@Order(1)
public class CustomerNotificationObserver implements BookingEventObserver {

    private final NotificationService notificationService;

    public CustomerNotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void update(BookingEvent event) {
        Booking booking = event.getBooking();
        Payment payment = event.getPayment();
        String ref = booking.getBookingReference();

        switch (event.getType()) {
            case BOOKING_CREATED -> notify(event, "Booking Submitted: " + ref,
                    "Your safari booking for " + booking.getSafariPackage().getTitle()
                            + " has been submitted. Status: PENDING validation by our Booking Manager.", "BOOKING");
            case BOOKING_UPDATED -> notify(event, "Booking Updated: " + ref,
                    "Your booking details were updated successfully.", "BOOKING");
            case BOOKING_APPROVED -> notify(event, "Booking Approved: " + ref,
                    "Good news! Your safari booking has been approved. Please proceed to payment to confirm your reservation.", "BOOKING");
            case BOOKING_REJECTED -> notify(event, "Booking Rejected: " + ref,
                    "We regret to inform you that your booking could not be approved. Reason: " + event.getDetail(), "BOOKING");
            case PAYMENT_CONFIRMED -> notify(event, "Payment Successful & Booking Confirmed!",
                    "Your payment of $" + payment.getAmount() + " was successful! Booking " + ref
                            + " is now CONFIRMED. Receipt Number: " + payment.getReceipt().getReceiptNumber(), "PAYMENT");
            case PAYMENT_FAILED -> notify(event, "Payment Failed: " + ref,
                    "Payment for your booking failed. Reason: " + payment.getFailureReason() + " You may retry payment now.", "PAYMENT");
            case BANK_TRANSFER_SUBMITTED -> notify(event, "Bank Transfer Submitted: " + ref,
                    "We received your bank transfer details (" + payment.getPaymentDetails() + "). Your booking will be confirmed as soon as "
                            + "our finance team verifies the payment, usually within one working day.", "PAYMENT");
            case BANK_TRANSFER_REJECTED -> notify(event, "Bank Transfer Not Verified: " + ref,
                    payment.getFailureReason() + " You can pay again with any payment method.", "PAYMENT");
            case REFUND_PROCESSED -> notify(event, "Refund Processed: " + event.getRefund().getRefundRef(),
                    "A refund of $" + event.getRefund().getAmount() + " has been processed for booking " + ref
                            + ". Reason: " + event.getRefund().getReason(), "PAYMENT");
            default -> { /* BANK_TRANSFER_VERIFIED: covered by PAYMENT_CONFIRMED; PAYMENT_REMOVED is internal to finance */ }
        }
    }

    private void notify(BookingEvent event, String title, String message, String type) {
        notificationService.createNotification(event.getCustomerUser(), title, message, type);
    }
}
