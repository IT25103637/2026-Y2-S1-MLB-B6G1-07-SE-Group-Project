package com.safari.wildlife.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "finance_manager_id", nullable = false)
    private User financeManager;

    @Column(name = "refund_ref", nullable = false, unique = true, length = 100)
    private String refundRef;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 30)
    private String status = "PROCESSED";

    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public Refund() {}

    public Refund(Payment payment, User financeManager, String refundRef, BigDecimal amount, String reason) {
        this.payment = payment;
        this.financeManager = financeManager;
        this.refundRef = refundRef;
        this.amount = amount;
        this.reason = reason;
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public User getFinanceManager() {
        return financeManager;
    }

    public void setFinanceManager(User financeManager) {
        this.financeManager = financeManager;
    }

    public String getRefundRef() {
        return refundRef;
    }

    public void setRefundRef(String refundRef) {
        this.refundRef = refundRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
