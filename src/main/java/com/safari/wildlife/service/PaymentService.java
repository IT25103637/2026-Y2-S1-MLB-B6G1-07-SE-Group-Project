package com.safari.wildlife.service;

import com.safari.wildlife.dto.PaymentRequestDto;
import com.safari.wildlife.dto.RefundRequestDto;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.enums.PaymentStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.PaymentProcessingException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.*;
import com.safari.wildlife.service.observer.BookingEvent;
import com.safari.wildlife.service.observer.BookingEventPublisher;
import com.safari.wildlife.service.observer.BookingEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final SafariScheduleRepository scheduleRepository;
    private final SecureRandom random = new SecureRandom();

    // DESIGN PATTERN: Observer Pattern - notifications, receipt e-mails and audit logs are handled by observers
    private final BookingEventPublisher bookingEvents;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          PaymentReceiptRepository receiptRepository,
                          RefundRepository refundRepository,
                          BookingRepository bookingRepository,
                          SafariScheduleRepository scheduleRepository,
                          BookingEventPublisher bookingEvents) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.bookingEvents = bookingEvents;
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaidAtDesc();
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for booking id: " + bookingId));
    }

    @Transactional(readOnly = true)
    public PaymentReceipt getReceiptByPaymentId(Long paymentId) {
        return receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for payment id: " + paymentId));
    }

    // ================= Payment methods =================
    // Stored in payments.payment_method. Instant methods confirm the booking immediately;
    // a bank transfer stays PENDING until the Finance Manager verifies it.
    public static final String CARD = "CARD";
    public static final String LANKAQR = "LANKAQR";
    public static final String BANK_TRANSFER = "BANK_TRANSFER";
    private static final Map<String, String> WALLETS = Map.of("EZCASH", "eZ Cash", "MCASH", "mCash", "GENIE", "Genie");

    // noRollbackFor: a declined attempt must still be saved (FAILED + reason) and the customer notified,
    // even though PaymentProcessingException is thrown back to the caller.
    @Transactional(noRollbackFor = PaymentProcessingException.class)
    public Payment processPayment(PaymentRequestDto dto, User customerUser) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));

        // A customer may only pay for their own booking (otherwise another account could, for example,
        // park a fake bank transfer on it and block the real owner from paying)
        User owner = booking.getCustomer() != null ? booking.getCustomer().getUser() : null;
        if (owner == null || customerUser == null || !owner.getId().equals(customerUser.getId())) {
            throw new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId());
        }

        // Check if booking is in approved state awaiting payment
        if (booking.getStatus() != BookingStatus.APPROVED_AWAITING_PAYMENT) {
            throw new BadRequestException("Payment can only be processed for approved bookings awaiting payment. Current status: " + booking.getStatus());
        }

        String method = dto.getPaymentMethod() == null || dto.getPaymentMethod().isBlank()
                ? CARD : dto.getPaymentMethod().trim().toUpperCase();

        // A booking has at most one payment row (payments.booking_id is UNIQUE): a failed attempt is re-used
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING && BANK_TRANSFER.equals(payment.getPaymentMethod())) {
            throw new BadRequestException("A bank transfer for this booking is already waiting for verification by our finance team.");
        }

        // Validate the fields of the chosen method and build a non-sensitive summary for the finance team
        String details;
        boolean declined;
        String declineReason;
        if (CARD.equals(method)) {
            String cardNum = digits(dto.getCardNumber());
            details = validateCard(dto, cardNum);
            declined = cardNum.endsWith("0000");                       // test card that is always declined
            declineReason = "Transaction declined: Insufficient funds or card security check failed. Please retry.";
        } else if (LANKAQR.equals(method)) {
            String ref = dto.getQrReference() == null ? "" : dto.getQrReference().trim().toUpperCase();
            if (!ref.matches("[A-Z0-9-]{0,40}")) {
                throw new BadRequestException("The LankaQR reference may only contain letters, digits and hyphens (max 40).");
            }
            details = ref.isEmpty() ? "LankaQR" : "LankaQR ref " + ref;
            declined = false;
            declineReason = "The LankaQR payment was not completed in the banking app.";
        } else if (WALLETS.containsKey(method)) {
            String mobile = normaliseSriLankanMobile(dto.getMobileNumber());
            String otp = dto.getOtp() == null ? "" : dto.getOtp().trim();
            if (!otp.matches("\\d{6}")) {
                throw new BadRequestException("Please enter the 6-digit one-time password sent to your phone.");
            }
            details = WALLETS.get(method) + " " + mobile.substring(0, 3) + "***" + mobile.substring(6);
            declined = otp.equals("000000");                            // test OTP that is always rejected
            declineReason = "Wallet payment declined: incorrect one-time password or insufficient wallet balance.";
        } else if (BANK_TRANSFER.equals(method)) {
            String bank = dto.getBankName() == null ? "" : dto.getBankName().trim();
            String ref = dto.getTransferReference() == null ? "" : dto.getTransferReference().trim().toUpperCase();
            if (!bank.matches("[A-Za-z0-9 .,'&()/-]{1,60}")) throw new BadRequestException("Please select the bank you transferred from.");
            if (!ref.matches("[A-Z0-9-]{6,30}")) {
                throw new BadRequestException("Please enter the transfer reference number from your bank (6-30 letters or digits).");
            }
            details = bank + " ref " + ref;
            declined = false;
            declineReason = null;
        } else {
            throw new BadRequestException("Unsupported payment method: " + method);
        }

        if (payment == null) {
            payment = new Payment(booking, newTransactionRef(method), method, booking.getTotalAmount());
        } else if (!method.equals(payment.getPaymentMethod())) {
            payment.setTransactionRef(newTransactionRef(method));           // a new attempt with another method
        }
        payment.setPaymentMethod(method);
        payment.setPaymentDetails(details);
        payment.setAmount(booking.getTotalAmount());

        // Scenario 6: payment failure (simulated decline) - recorded, customer notified, may retry
        if (dto.isSimulateFailure() || declined) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(declineReason);
            Payment failed = paymentRepository.save(payment);

            bookingEvents.notifyObservers(new BookingEvent(BookingEventType.PAYMENT_FAILED, booking, customerUser)
                    .withPayment(failed));

            throw new PaymentProcessingException("Payment failed. " + declineReason);
        }

        // Bank transfer: money arrives later, so the finance team must verify it first
        if (BANK_TRANSFER.equals(method)) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(null);
            payment.setPaidAt(null);
            Payment saved = paymentRepository.save(payment);

            bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BANK_TRANSFER_SUBMITTED, booking, customerUser)
                    .withPayment(saved));
            return saved;
        }

        // Success Scenario 5 (card, LankaQR, mobile wallet)
        return completeSuccessfulPayment(payment, booking, customerUser);
    }

    /** Finance Manager confirms that a bank transfer has arrived: payment PAID, booking CONFIRMED, receipt issued. */
    @Transactional
    public Payment verifyBankTransfer(Long paymentId, User financeManager) {
        Payment payment = getPaymentById(paymentId);
        if (!BANK_TRANSFER.equals(payment.getPaymentMethod()) || payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only bank transfers that are waiting for verification can be verified.");
        }
        Booking booking = payment.getBooking();
        if (booking.getStatus() != BookingStatus.APPROVED_AWAITING_PAYMENT) {
            throw new BadRequestException("This booking is no longer awaiting payment (status: " + booking.getStatus()
                    + "). Reject the transfer and refund the customer instead.");
        }
        Payment saved = completeSuccessfulPayment(payment, booking, financeManager);
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BANK_TRANSFER_VERIFIED, booking, financeManager)
                .withPayment(saved));
        return saved;
    }

    /** Finance Manager could not find the transfer: payment FAILED with a reason, customer may pay again. */
    @Transactional
    public Payment rejectBankTransfer(Long paymentId, User financeManager, String reason) {
        Payment payment = getPaymentById(paymentId);
        if (!BANK_TRANSFER.equals(payment.getPaymentMethod()) || payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only bank transfers that are waiting for verification can be rejected.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Please give a reason so the customer knows what to fix.");
        }
        String text = "Bank transfer could not be verified: " + reason.trim();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(text.length() > 255 ? text.substring(0, 255) : text);
        Payment saved = paymentRepository.save(payment);

        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BANK_TRANSFER_REJECTED, saved.getBooking(), financeManager)
                .withPayment(saved));
        return saved;
    }

    // ---------------- helpers ----------------

    private String newTransactionRef(String method) {
        String prefix = switch (method) {
            case LANKAQR -> "QR";
            case BANK_TRANSFER -> "BT";
            case CARD -> "TXN";
            default -> "MW";                                            // mobile wallets
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    /** Validates the card fields and returns e.g. "Visa **** 3456" (never the full number). */
    private String validateCard(PaymentRequestDto dto, String cardNum) {
        if (dto.getCardHolderName() == null || dto.getCardHolderName().isBlank()) {
            throw new BadRequestException("Please enter the name on the card.");
        }
        if (!cardNum.matches("\\d{13,19}")) {
            throw new BadRequestException("Invalid payment card number format.");
        }
        if (dto.getCvv() == null || !dto.getCvv().trim().matches("\\d{3,4}")) {
            throw new BadRequestException("Invalid CVV security code.");
        }
        String expiry = dto.getExpiryDate() == null ? "" : dto.getExpiryDate().trim();
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new BadRequestException("Expiry date must be in MM/YY format.");
        }
        java.time.YearMonth exp = java.time.YearMonth.of(2000 + Integer.parseInt(expiry.substring(3)), Integer.parseInt(expiry.substring(0, 2)));
        if (exp.isBefore(java.time.YearMonth.now())) {
            throw new BadRequestException("This card has expired.");
        }
        String brand = cardNum.startsWith("4") ? "Visa"
                : cardNum.matches("5[1-5].*|2[2-7].*") ? "Mastercard"
                : cardNum.matches("3[47].*") ? "Amex" : "Card";
        return brand + " **** " + cardNum.substring(cardNum.length() - 4);
    }

    /** Accepts 07XXXXXXXX, 7XXXXXXXX or +94 7XXXXXXXX and returns 07XXXXXXXX. */
    private static String normaliseSriLankanMobile(String value) {
        String n = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (n.startsWith("+94")) n = "0" + n.substring(3);
        else if (n.startsWith("94") && n.length() == 11) n = "0" + n.substring(2);
        else if (n.startsWith("7") && n.length() == 9) n = "0" + n;
        if (!n.matches("07\\d{8}")) {
            throw new BadRequestException("Please enter a valid Sri Lankan mobile number, e.g. 077 123 4567.");
        }
        return n;
    }

    /** Shared success path: PAID, booking CONFIRMED, receipt with 8% VAT; observers send the PDF e-mail, notification and audit. */
    private Payment completeSuccessfulPayment(Payment payment, Booking booking, User actor) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        Payment savedPayment = paymentRepository.save(payment);

        // Update booking to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Generate formal receipt with tax calculation
        BigDecimal taxRate = new BigDecimal("0.08"); // 8% VAT
        BigDecimal total = savedPayment.getAmount();
        BigDecimal subtotal = total.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal taxVat = total.subtract(subtotal);

        String receiptNum = "REC-SLS-" + (10000 + random.nextInt(90000));
        PaymentReceipt receipt = new PaymentReceipt(savedPayment, receiptNum, subtotal, taxVat, total);
        receiptRepository.save(receipt);
        savedPayment.setReceipt(receipt);   // so observers and the caller can use the receipt number straight away

        // Observer Pattern: PDF receipt e-mail, customer notification and audit log are handled by the observers
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.PAYMENT_CONFIRMED, booking, actor)
                .withPayment(savedPayment));

        return savedPayment;
    }

    @Transactional
    public Refund processRefund(RefundRequestDto dto, User financeManager) {
        Payment payment = getPaymentById(dto.getPaymentId());

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Refund can only be issued on completed PAID payments. Current status: " + payment.getStatus());
        }

        if (dto.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed original payment amount of $" + payment.getAmount());
        }

        String refundRef = "REF-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));
        Refund refund = new Refund(payment, financeManager, refundRef, dto.getAmount(), dto.getReason().trim());
        refundRepository.save(refund);

        // Update payment & booking status
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.REFUNDED);

        // Release schedule slots on cancellation / refund
        SafariSchedule schedule = booking.getSchedule();
        schedule.releaseSlots(booking.getTotalGuests());
        scheduleRepository.save(schedule);

        bookingRepository.save(booking);

        // Observer Pattern: customer notification + audit log are handled by the observers
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.REFUND_PROCESSED, booking, financeManager)
                .withPayment(payment)
                .withRefund(refund));

        return refund;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFinancialSummary() {
        List<Payment> allPayments = paymentRepository.findAll();
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        int paidCount = 0;
        int refundCount = 0;

        for (Payment p : allPayments) {
            if (p.getStatus() == PaymentStatus.PAID) {
                totalPaid = totalPaid.add(p.getAmount());
                paidCount++;
            } else if (p.getStatus() == PaymentStatus.REFUNDED) {
                totalRefunded = totalRefunded.add(p.getAmount());
                refundCount++;
            }
        }

        BigDecimal netRevenue = totalPaid; // Refunds were already deducted or separated

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", totalPaid);
        summary.put("totalRefunded", totalRefunded);
        summary.put("netRevenue", netRevenue);
        summary.put("paidTransactionsCount", paidCount);
        summary.put("refundTransactionsCount", refundCount);
        summary.put("totalTransactionsCount", allPayments.size());
        summary.put("pendingTransfersCount", allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && BANK_TRANSFER.equals(p.getPaymentMethod()))
                .count());

        return summary;
    }
}
