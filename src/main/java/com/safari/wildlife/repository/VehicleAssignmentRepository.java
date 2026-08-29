package com.safari.wildlife.repository;

import com.safari.wildlife.entity.VehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, Long> {
    List<VehicleAssignment> findByBookingId(Long bookingId);
    Optional<VehicleAssignment> findFirstByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    boolean existsByBookingIdAndVehicleId(Long bookingId, Long vehicleId);

    @Query("SELECT COUNT(va) > 0 FROM VehicleAssignment va WHERE va.vehicle.id = :vehicleId " +
           "AND va.booking.schedule.scheduleDate = :scheduleDate " +
           "AND va.booking.schedule.timeSlot = :timeSlot " +
           "AND va.status = 'ACTIVE'")
    boolean isVehicleBusyAtSlot(@Param("vehicleId") Long vehicleId,
                                @Param("scheduleDate") LocalDate scheduleDate,
                                @Param("timeSlot") String timeSlot);
}
