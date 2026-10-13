package com.safari.wildlife.service;

import com.safari.wildlife.dto.AssignmentDto;
import com.safari.wildlife.dto.GuideDto;
import com.safari.wildlife.dto.MaintenanceDto;
import com.safari.wildlife.dto.VehicleDto;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.GuideStatus;
import com.safari.wildlife.enums.VehicleStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.exception.VehicleUnavailableException;
import com.safari.wildlife.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FleetService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final GuideRepository guideRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final GuideAssignmentRepository guideAssignmentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Autowired
    public FleetService(VehicleRepository vehicleRepository,
                        MaintenanceRecordRepository maintenanceRepository,
                        GuideRepository guideRepository,
                        VehicleAssignmentRepository vehicleAssignmentRepository,
                        GuideAssignmentRepository guideAssignmentRepository,
                        BookingRepository bookingRepository,
                        NotificationService notificationService,
                        AuditService auditService) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.guideRepository = guideRepository;
        this.vehicleAssignmentRepository = vehicleAssignmentRepository;
        this.guideAssignmentRepository = guideAssignmentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ================= Vehicle Management =================

    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    /** Registration number and model are mandatory for every vehicle (create and edit). */
    private void validateVehicleDetails(VehicleDto dto) {
        String reg = dto.getRegistrationNumber() == null ? "" : dto.getRegistrationNumber().trim();
        if (reg.isEmpty()) {
            throw new BadRequestException("Registration number is required.");
        }
        if (!reg.matches("[A-Za-z0-9][A-Za-z0-9 -]{1,19}")) {
            throw new BadRequestException("Registration number may contain only letters, numbers, spaces and hyphens (2-20 characters), e.g. WP-SP-9999.");
        }
        if (dto.getModelName() == null || dto.getModelName().trim().isEmpty()) {
            throw new BadRequestException("Vehicle model name is required.");
        }
    }

    @Transactional
    public Vehicle registerVehicle(VehicleDto dto, User fleetManager) {
        validateVehicleDetails(dto);
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(dto.getRegistrationNumber().trim())) {
            throw new BadRequestException("Vehicle with registration number '" + dto.getRegistrationNumber().trim() + "' is already registered.");
        }

        Vehicle vehicle = new Vehicle(
                dto.getRegistrationNumber().trim().toUpperCase(),
                dto.getModelName().trim(),
                dto.getSeatingCapacity() != null ? dto.getSeatingCapacity() : 6,
                dto.getModelYear(),
                dto.getAirConditioned() != null ? dto.getAirConditioned() : false
        );
        if (dto.getStatus() != null) {
            vehicle.setStatus(dto.getStatus());
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        auditService.logAction(fleetManager, "VEHICLE_REGISTER", "Vehicle",
                saved.getRegistrationNumber(), "Registered vehicle: " + saved.getModelName(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public Vehicle updateVehicle(Long id, VehicleDto dto, User fleetManager) {
        Vehicle vehicle = getVehicleById(id);
        validateVehicleDetails(dto);

        if (vehicleRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(dto.getRegistrationNumber().trim(), id)) {
            throw new BadRequestException("Another vehicle with registration '" + dto.getRegistrationNumber().trim() + "' already exists.");
        }

        vehicle.setRegistrationNumber(dto.getRegistrationNumber().trim().toUpperCase());
        vehicle.setModelName(dto.getModelName().trim());
        if (dto.getSeatingCapacity() != null) {
            vehicle.setSeatingCapacity(dto.getSeatingCapacity());
        }
        vehicle.setModelYear(dto.getModelYear());
        if (dto.getAirConditioned() != null) {
            vehicle.setIsAirConditioned(dto.getAirConditioned());
        }
        if (dto.getStatus() != null) {
            vehicle.setStatus(dto.getStatus());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        auditService.logAction(fleetManager, "VEHICLE_UPDATE", "Vehicle",
                updated.getRegistrationNumber(), "Updated vehicle details", "127.0.0.1");

        return updated;
    }

    // ================= Maintenance =================

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> getVehicleMaintenance(Long vehicleId) {
        return maintenanceRepository.findByVehicleIdOrderByStartDateDesc(vehicleId);
    }

    @Transactional
    public MaintenanceRecord recordMaintenance(MaintenanceDto dto, User fleetManager) {
        Vehicle vehicle = getVehicleById(dto.getVehicleId());

        MaintenanceRecord record = new MaintenanceRecord(
                vehicle,
                dto.getMaintenanceType().trim(),
                dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now(),
                dto.getExpectedEndDate(),
                dto.getCost(),
                dto.getDescription()
        );
        MaintenanceRecord saved = maintenanceRepository.save(record);

        // Vehicle is locked under maintenance
        vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        vehicleRepository.save(vehicle);

        auditService.logAction(fleetManager, "MAINTENANCE_LOG", "MaintenanceRecord",
                String.valueOf(saved.getId()), "Vehicle " + vehicle.getRegistrationNumber() + " placed under maintenance", "127.0.0.1");

        return saved;
    }

    @Transactional
    public MaintenanceRecord completeMaintenance(Long maintenanceId, User fleetManager) {
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));

        record.setCompleted(true);
        record.setActualEndDate(LocalDate.now());
        maintenanceRepository.save(record);

        Vehicle vehicle = record.getVehicle();
        // If no more open maintenance tasks, restore to AVAILABLE
        if (!maintenanceRepository.existsByVehicleIdAndIsCompletedFalse(vehicle.getId())) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        auditService.logAction(fleetManager, "MAINTENANCE_COMPLETE", "MaintenanceRecord",
                String.valueOf(maintenanceId), "Completed maintenance for vehicle " + vehicle.getRegistrationNumber(), "127.0.0.1");

        return record;
    }

    // ================= Guides =================

    @Transactional(readOnly = true)
    public List<Guide> getAllGuides() {
        return guideRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Guide> getAvailableGuides() {
        return guideRepository.findByStatus(GuideStatus.AVAILABLE);
    }

    @Transactional
    public Guide registerGuide(GuideDto dto, User fleetManager) {
        if (guideRepository.existsByLicenseNumberIgnoreCase(dto.getLicenseNumber().trim())) {
            throw new BadRequestException("Guide with license number '" + dto.getLicenseNumber().trim() + "' already exists.");
        }

        Guide guide = new Guide(
                dto.getFullName().trim(),
                dto.getLicenseNumber().trim().toUpperCase(),
                dto.getLanguagesSpoken(),
                dto.getPhone(),
                dto.getExperienceYears()
        );
        if (dto.getStatus() != null) {
            guide.setStatus(dto.getStatus());
        }

        Guide saved = guideRepository.save(guide);
        auditService.logAction(fleetManager, "GUIDE_REGISTER", "Guide",
                saved.getLicenseNumber(), "Registered guide: " + saved.getFullName(), "127.0.0.1");

        return saved;
    }

    // ================= Dispatch & Assignments =================

    @Transactional
    public VehicleAssignment assignVehicleToBooking(Long bookingId, Long vehicleId, User fleetManager) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        Vehicle vehicle = getVehicleById(vehicleId);

        // Verification for Scenario 11: Vehicle under maintenance cannot be assigned
        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
            throw new VehicleUnavailableException("Vehicle " + vehicle.getRegistrationNumber() +
                    " is currently UNDER MAINTENANCE and cannot be assigned to any safari trip.");
        }

        // Verification for vehicle allocation conflict
        SafariSchedule schedule = booking.getSchedule();
        if (vehicleAssignmentRepository.isVehicleBusyAtSlot(vehicle.getId(), schedule.getScheduleDate(), schedule.getTimeSlot())) {
            throw new VehicleUnavailableException("Vehicle allocation conflict: " + vehicle.getRegistrationNumber() +
                    " is already assigned to another safari trip on " + schedule.getScheduleDate() +
                    " during slot " + schedule.getTimeSlot());
        }

        // Check capacity
        if (vehicle.getSeatingCapacity() < booking.getTotalGuests()) {
            throw new BadRequestException("Vehicle seating capacity (" + vehicle.getSeatingCapacity() +
                    ") is insufficient for booking group of " + booking.getTotalGuests() + " guests.");
        }

        VehicleAssignment assignment = new VehicleAssignment(booking, vehicle, fleetManager);
        VehicleAssignment saved = vehicleAssignmentRepository.save(assignment);

        auditService.logAction(fleetManager, "VEHICLE_ASSIGN", "VehicleAssignment",
                String.valueOf(saved.getId()), "Assigned vehicle " + vehicle.getRegistrationNumber() + " to booking " + booking.getBookingReference(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public GuideAssignment assignGuideToBooking(Long bookingId, Long guideId, User fleetManager) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("Guide not found with id: " + guideId));

        if (guide.getStatus() == GuideStatus.ON_LEAVE) {
            throw new BadRequestException("Guide " + guide.getFullName() + " is currently ON LEAVE and cannot be assigned.");
        }

        SafariSchedule schedule = booking.getSchedule();
        if (guideAssignmentRepository.isGuideBusyAtSlot(guide.getId(), schedule.getScheduleDate(), schedule.getTimeSlot())) {
            throw new BadRequestException("Guide allocation conflict: " + guide.getFullName() +
                    " is already assigned to a safari trip on " + schedule.getScheduleDate() +
                    " during slot " + schedule.getTimeSlot());
        }

        GuideAssignment assignment = new GuideAssignment(booking, guide, fleetManager);
        GuideAssignment saved = guideAssignmentRepository.save(assignment);

        auditService.logAction(fleetManager, "GUIDE_ASSIGN", "GuideAssignment",
                String.valueOf(saved.getId()), "Assigned guide " + guide.getFullName() + " to booking " + booking.getBookingReference(), "127.0.0.1");

        return saved;
    }

    @Transactional(readOnly = true)
    public List<VehicleAssignment> getAssignmentsForBooking(Long bookingId) {
        return vehicleAssignmentRepository.findByBookingId(bookingId);
    }

    public Vehicle updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    public com.safari.wildlife.entity.Guide updateGuideStatus(Long id, com.safari.wildlife.enums.GuideStatus status) {
        com.safari.wildlife.entity.Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guide not found with id: " + id));

        guide.setStatus(status);
        return guideRepository.save(guide);
    }
    @Transactional
    public com.safari.wildlife.entity.Guide updateGuide(Long id, com.safari.wildlife.dto.GuideDto dto, com.safari.wildlife.entity.User manager) {
        com.safari.wildlife.entity.Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guide not found"));

        guide.setFullName(dto.getFullName());
        guide.setLicenseNumber(dto.getLicenseNumber());
        guide.setLanguagesSpoken(dto.getLanguagesSpoken());
        guide.setExperienceYears(dto.getExperienceYears());

        // ADD THIS LINE: Explicitly map the phone number from the DTO
        guide.setPhone(dto.getPhone());

        if (dto.getStatus() != null) {
            guide.setStatus(dto.getStatus());
        }

        com.safari.wildlife.entity.Guide updated = guideRepository.save(guide);

        auditService.logAction(manager, "GUIDE_UPDATE", "Guide",
                updated.getLicenseNumber(), "Updated guide details for: " + updated.getFullName(), "127.0.0.1");

        return updated;
    }

    public void deleteGuide(Long id) {
        guideRepository.deleteById(id);
    }

}
