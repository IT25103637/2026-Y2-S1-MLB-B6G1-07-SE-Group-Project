package com.safari.wildlife.repository;

import com.safari.wildlife.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    Optional<PaymentReceipt> findByReceiptNumber(String receiptNumber);
    Optional<PaymentReceipt> findByPaymentId(Long paymentId);
}
