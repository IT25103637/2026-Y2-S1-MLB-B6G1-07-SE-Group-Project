package com.safari.wildlife.repository;

import com.safari.wildlife.entity.SafariSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafariScheduleRepository extends JpaRepository<SafariSchedule, Long> {
    Optional<SafariSchedule> findByDestinationIdAndScheduleDateAndTimeSlot(Long destinationId, LocalDate scheduleDate, String timeSlot);
    boolean existsByDestinationIdAndScheduleDateAndTimeSlot(Long destinationId, LocalDate scheduleDate, String timeSlot);
    boolean existsByDestinationIdAndScheduleDateAndTimeSlotAndIdNot(Long destinationId, LocalDate scheduleDate, String timeSlot, Long id);
    List<SafariSchedule> findByDestinationIdAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(Long destinationId, LocalDate scheduleDate);
    List<SafariSchedule> findByScheduleDateGreaterThanEqualOrderByScheduleDateAsc(LocalDate scheduleDate);
    List<SafariSchedule> findByDestinationId(Long destinationId);
}
