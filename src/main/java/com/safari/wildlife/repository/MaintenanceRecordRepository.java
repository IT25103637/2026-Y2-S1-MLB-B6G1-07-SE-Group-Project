package com.safari.wildlife.repository;

import com.safari.wildlife.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByVehicleIdOrderByStartDateDesc(Long vehicleId);
    List<MaintenanceRecord> findByIsCompletedFalse();
    boolean existsByVehicleIdAndIsCompletedFalse(Long vehicleId);
}
