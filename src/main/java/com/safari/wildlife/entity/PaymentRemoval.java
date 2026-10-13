package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit record written when a Finance Manager removes a payment from the finance ledger.
 * The payment row itself is kept (Payment.removed = true); this table records which payment
 * was removed, when, by whom and why, plus a snapshot of the key payment details.
 */
@Entity
@Table(name = "payment_removals")
public class PaymentRemoval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    @JsonIgnoreProperties({"booking", "receipt", "refund", "hibernateLazyInitializer", "handler"})
    private Payment payment;

    @Column(name = "removed_at", nullable = false)
    private LocalDateTime removedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "removed_by", nullable = false)
    @JsonIgnoreProperties({"role", "hibernateLazyInitializer", "handler"})
    private User removedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    // snapshot of the payment at the time it was removed
    @Column(name = "transaction_ref", nullable = false, length = 100)
    private String transactionRef;

    @Column(name = "booking_reference", length = 30)
    private String bookingReference;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    public PaymentRemoval() {}

    public PaymentRemoval(Payment payment, User removedBy, String reason) {
        this.payment = payment;
        this.removedBy = removedBy;
        this.reason = reason;
        this.transactionRef = payment.getTransactionRef();
        this.bookingReference = payment.getBooking() != null ? payment.getBooking().getBookingReference() : null;
        this.amount = payment.getAmount();
        this.paymentStatus = payment.getStatus().name();
        this.removedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public LocalDateTime getRemovedAt() { return removedAt; }
    public User getRemovedBy() { return removedBy; }
    public String getReason() { return reason; }
    public String getTransactionRef() { return transactionRef; }
    public String getBookingReference() { return bookingReference; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentStatus() { return paymentStatus; }
}
