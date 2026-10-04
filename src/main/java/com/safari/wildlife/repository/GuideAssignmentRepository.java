package com.safari.wildlife.repository;

import com.safari.wildlife.entity.GuideAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuideAssignmentRepository extends JpaRepository<GuideAssignment, Long> {
    List<GuideAssignment> findByBookingId(Long bookingId);
    Optional<GuideAssignment> findFirstByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    boolean existsByBookingIdAndGuideId(Long bookingId, Long guideId);

    @Query("SELECT COUNT(ga) > 0 FROM GuideAssignment ga WHERE ga.guide.id = :guideId " +
           "AND ga.booking.schedule.scheduleDate = :scheduleDate " +
           "AND ga.booking.schedule.timeSlot = :timeSlot " +
           "AND ga.status = 'ACTIVE'")
    boolean isGuideBusyAtSlot(@Param("guideId") Long guideId,
                              @Param("scheduleDate") LocalDate scheduleDate,
                              @Param("timeSlot") String timeSlot);
}
