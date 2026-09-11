package com.safari.wildlife.repository;

import com.safari.wildlife.entity.BookingCustomization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingCustomizationRepository extends JpaRepository<BookingCustomization, Long> {
    List<BookingCustomization> findByBookingId(Long bookingId);
}
