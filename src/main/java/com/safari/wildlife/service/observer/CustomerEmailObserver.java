package com.safari.wildlife.service.observer;

import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.service.EmailService;
import com.safari.wildlife.service.PdfReceiptService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Concrete Observer: e-mails the customer when the booking is approved and when it is paid (PDF receipt). */
@Component
@Order(2)
public class CustomerEmailObserver implements BookingEventObserver {

    private final EmailService emailService;
    private final PdfReceiptService pdfReceiptService;

    public CustomerEmailObserver(EmailService emailService, PdfReceiptService pdfReceiptService) {
        this.emailService = emailService;
        this.pdfReceiptService = pdfReceiptService;
    }

    @Override
    public void update(BookingEvent event) {
        Booking booking = event.getBooking();
        User customer = event.getCustomerUser();
        // An e-mail problem must never undo the approval or the payment, so failures are only logged
        try {
            switch (event.getType()) {
                case BOOKING_APPROVED -> emailService.sendBookingApprovalEmail(
                        customer.getEmail(), customer.getFullName(), booking.getBookingReference());
                case PAYMENT_CONFIRMED -> {
                    byte[] receiptPdf = pdfReceiptService.generateReceipt(
                            customer.getFullName(),
                            booking.getBookingReference(),
                            booking.getSafariPackage().getTitle(),
                            event.getPayment().getAmount().doubleValue());
                    emailService.sendBookingReceiptEmail(
                            customer.getEmail(), customer.getFullName(), booking.getBookingReference(), receiptPdf);
                }
                default -> { /* no e-mail for the other events */ }
            }
        } catch (Exception e) {
            System.err.println("Could not send e-mail for " + event.getType() + " (" + booking.getBookingReference() + "): " + e.getMessage());
        }
    }
}
