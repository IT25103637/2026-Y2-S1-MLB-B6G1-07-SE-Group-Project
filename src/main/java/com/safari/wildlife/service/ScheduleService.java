package com.safari.wildlife.service;

import com.safari.wildlife.dto.ScheduleDto;
import com.safari.wildlife.entity.Destination;
import com.safari.wildlife.entity.SafariPackage;
import com.safari.wildlife.entity.SafariSchedule;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.exception.ScheduleConflictException;
import com.safari.wildlife.repository.DestinationRepository;
import com.safari.wildlife.repository.SafariPackageRepository;
import com.safari.wildlife.repository.SafariScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleService {

    private final SafariScheduleRepository scheduleRepository;
    private final DestinationRepository destinationRepository;
    private final SafariPackageRepository packageRepository;
    private final AuditService auditService;
    private final AuthService authService;

    @Autowired
    public ScheduleService(SafariScheduleRepository scheduleRepository,
                           DestinationRepository destinationRepository,
                           SafariPackageRepository packageRepository,
                           AuditService auditService,
                           AuthService authService) {
        this.scheduleRepository = scheduleRepository;
        this.destinationRepository = destinationRepository;
        this.packageRepository = packageRepository;
        this.auditService = auditService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<SafariSchedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SafariSchedule> getUpcomingSchedules() {
        return scheduleRepository.findByScheduleDateGreaterThanEqualOrderByScheduleDateAsc(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<SafariSchedule> getSchedulesByDestination(Long destinationId) {
        return scheduleRepository.findByDestinationIdAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(destinationId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public SafariSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Safari schedule not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public boolean checkScheduleConflict(Long destinationId, LocalDate date, String timeSlot, Long excludeScheduleId) {
        if (excludeScheduleId != null) {
            return scheduleRepository.existsByDestinationIdAndScheduleDateAndTimeSlotAndIdNot(destinationId, date, timeSlot, excludeScheduleId);
        }
        return scheduleRepository.existsByDestinationIdAndScheduleDateAndTimeSlot(destinationId, date, timeSlot);
    }

    /** Maximum number of guests a single safari schedule can hold. */
    public static final int MAX_GUESTS_PER_SCHEDULE = 40;

    /** Capacity must be 1-40 and can never be set below the seats that are already booked. */
    private void validateCapacity(Integer maxSlots, int bookedSlots) {
        if (maxSlots == null || maxSlots < 1 || maxSlots > MAX_GUESTS_PER_SCHEDULE) {
            throw new BadRequestException("Maximum capacity must be between 1 and " + MAX_GUESTS_PER_SCHEDULE + " guests.");
        }
        if (maxSlots < bookedSlots) {
            throw new BadRequestException("Maximum capacity cannot be lower than the " + bookedSlots
                    + " seats that are already booked on this schedule.");
        }
    }

    @Transactional
    public SafariSchedule createSchedule(ScheduleDto dto) {
        Destination destination = destinationRepository.findById(dto.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + dto.getDestinationId()));

        SafariPackage safariPackage = packageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Safari package not found with id: " + dto.getPackageId()));

        if (dto.getScheduleDate() == null) {
            throw new BadRequestException("Schedule date is required.");
        }
        if (dto.getTimeSlot() == null || dto.getTimeSlot().trim().isEmpty()) {
            throw new BadRequestException("Time slot is required.");
        }

        validateCapacity(dto.getMaxSlots() != null ? dto.getMaxSlots() : 20, 0);

        // Conflict check: Scenario 9 prevention
        if (checkScheduleConflict(destination.getId(), dto.getScheduleDate(), dto.getTimeSlot(), null)) {
            throw new ScheduleConflictException("Schedule conflict detected: A safari schedule already exists for "
                    + destination.getName() + " on " + dto.getScheduleDate() + " during slot " + dto.getTimeSlot());
        }

        SafariSchedule schedule = new SafariSchedule(
                destination,
                safariPackage,
                dto.getScheduleDate(),
                dto.getTimeSlot(),
                dto.getMaxSlots() != null ? dto.getMaxSlots() : 20
        );
        if (dto.getStatus() != null) {
            schedule.setStatus(dto.getStatus());
        }

        SafariSchedule saved = scheduleRepository.save(schedule);
        auditService.logAction(authService.getCurrentUser(), "SCHEDULE_CREATE", "SafariSchedule",
                String.valueOf(saved.getId()), "Created schedule for " + destination.getName() + " on " + saved.getScheduleDate(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public SafariSchedule updateSchedule(Long id, ScheduleDto dto) {
        SafariSchedule schedule = getScheduleById(id);

        if (dto.getScheduleDate() == null) {
            throw new BadRequestException("Schedule date is required.");
        }
        if (dto.getTimeSlot() == null || dto.getTimeSlot().trim().isEmpty()) {
            throw new BadRequestException("Time slot is required.");
        }
        validateCapacity(dto.getMaxSlots(), schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0);

        if (checkScheduleConflict(schedule.getDestination().getId(), dto.getScheduleDate(), dto.getTimeSlot(), id)) {
            throw new ScheduleConflictException("Schedule conflict detected: Another safari schedule already exists for "
                    + schedule.getDestination().getName() + " on " + dto.getScheduleDate() + " during slot " + dto.getTimeSlot());
        }

        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setTimeSlot(dto.getTimeSlot());
        if (dto.getMaxSlots() != null) {
            schedule.setMaxSlots(dto.getMaxSlots());
        }
        if (dto.getStatus() != null) {
            schedule.setStatus(dto.getStatus());
        }

        SafariSchedule updated = scheduleRepository.save(schedule);
        auditService.logAction(authService.getCurrentUser(), "SCHEDULE_UPDATE", "SafariSchedule",
                String.valueOf(updated.getId()), "Updated schedule ID: " + updated.getId(), "127.0.0.1");

        return updated;
    }

    @Transactional
    public void deleteSchedule(Long id) {
        SafariSchedule schedule = getScheduleById(id);
        if (schedule.getBookedSlots() > 0) {
            throw new BadRequestException("Cannot delete schedule with active bookings. Please cancel or reassign bookings first.");
        }
        scheduleRepository.delete(schedule);
        auditService.logAction(authService.getCurrentUser(), "SCHEDULE_DELETE", "SafariSchedule",
                String.valueOf(id), "Deleted schedule ID: " + id, "127.0.0.1");
    }
}
