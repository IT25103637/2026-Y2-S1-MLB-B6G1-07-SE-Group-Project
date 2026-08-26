package com.safari.wildlife.repository;

import com.safari.wildlife.entity.Payment;
import com.safari.wildlife.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionRef(String transactionRef);
    List<Payment> findByStatusOrderByPaidAtDesc(PaymentStatus status);
    List<Payment> findAllByOrderByPaidAtDesc();
    long countByStatus(PaymentStatus status);
}
