package com.safari.wildlife.repository;

import com.safari.wildlife.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundRef(String refundRef);
    List<Refund> findByPaymentId(Long paymentId);
    List<Refund> findAllByOrderByProcessedAtDesc();
}
