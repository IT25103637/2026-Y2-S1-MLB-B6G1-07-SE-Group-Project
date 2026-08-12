package com.safari.wildlife.repository;

import com.safari.wildlife.entity.BookingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {
    List<BookingApproval> findByBookingIdOrderByActionDateDesc(Long bookingId);
}
