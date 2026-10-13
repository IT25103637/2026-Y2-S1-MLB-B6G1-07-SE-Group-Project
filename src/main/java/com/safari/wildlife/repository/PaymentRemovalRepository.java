package com.safari.wildlife.repository;

import com.safari.wildlife.entity.PaymentRemoval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRemovalRepository extends JpaRepository<PaymentRemoval, Long> {
    List<PaymentRemoval> findAllByOrderByRemovedAtDesc();
}
