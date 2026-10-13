package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safari.wildlife.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prevents Payment -> Booking -> Payment loop
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"payment", "hibernateLazyInitializer", "handler"})
    private Booking booking;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
    private String transactionRef;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // CARD, LANKAQR, EZCASH, MCASH, GENIE, BANK_TRANSFER

    // Non-sensitive summary of how the customer paid, e.g. "Visa **** 3456",
    // "eZ Cash 077***4567" or "Sampath Bank ref TRF889201" (shown to the finance team)
    @Column(name = "payment_details", length = 150)
    private String paymentDetails;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Prevents Payment -> PaymentReceipt -> Payment loop
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"payment", "hibernateLazyInitializer", "handler"})
    private PaymentReceipt receipt;

    // ADD THIS NEW BLOCK: Handles cascading deletes for Refunds
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties({"payment", "hibernateLazyInitializer", "handler"})
    private Refund refund;

    // Removed from the finance ledger by a Finance Manager. The row is kept (soft delete);
    // payment_removals records who removed it, when and why.
    @JsonIgnore
    @Column(name = "removed")
    private Boolean removed = false;

    public Payment() {}

    public Payment(Booking booking, String transactionRef, String paymentMethod, BigDecimal amount) {
        this.booking = booking;
        this.transactionRef = transactionRef;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public PaymentReceipt getReceipt() {
        return receipt;
    }

    public void setReceipt(PaymentReceipt receipt) {
        this.receipt = receipt;
    }

    public Refund getRefund() {
        return refund;
    }

    public void setRefund(Refund refund) {
        this.refund = refund;
    }

    @JsonIgnore
    public boolean isRemoved() {
        return Boolean.TRUE.equals(removed);            // rows created before this column existed hold NULL
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}