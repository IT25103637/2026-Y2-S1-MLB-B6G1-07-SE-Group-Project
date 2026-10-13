package com.safari.wildlife.controller;

import com.safari.wildlife.dto.DestinationDto;
import com.safari.wildlife.dto.ScheduleDto;
import com.safari.wildlife.entity.Destination;
import com.safari.wildlife.entity.SafariSchedule;
import com.safari.wildlife.service.DestinationService;
import com.safari.wildlife.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.safari.wildlife.repository.SafariPackageRepository;
import com.safari.wildlife.entity.SafariPackage;
import com.safari.wildlife.enums.SafariType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operations")
public class OperationsApiController {

    @Autowired
    private com.safari.wildlife.repository.SafariPackageRepository packageRepository;

    @Autowired
    private com.safari.wildlife.repository.SafariScheduleRepository scheduleRepository;

    @Autowired
    private SafariPackageRepository safariPackageRepository;

    // ADDED: Injecting DestinationRepository for direct manual cascading
    @Autowired
    private com.safari.wildlife.repository.DestinationRepository destinationRepository;

    private final DestinationService destinationService;
    private final ScheduleService scheduleService;

    @Autowired
    public OperationsApiController(DestinationService destinationService, ScheduleService scheduleService) {
        this.destinationService = destinationService;
        this.scheduleService = scheduleService;
    }

    // ================= Destinations =================

    @GetMapping("/destinations")
    public ResponseEntity<List<Destination>> getAllDestinations() {
        return ResponseEntity.ok(destinationService.getAllDestinations());
    }

    @PostMapping("/destinations")
    public ResponseEntity<Map<String, Object>> createDestination(@Valid @RequestBody DestinationDto dto) {
        Destination destination = destinationService.createDestination(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Destination '" + destination.getName() + "' created successfully.");
        response.put("destinationId", destination.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/destinations/{id}")
    public ResponseEntity<Map<String, Object>> updateDestination(@PathVariable Long id, @Valid @RequestBody DestinationDto dto) {
        Destination updated = destinationService.updateDestination(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Destination '" + updated.getName() + "' updated successfully.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/destinations/{id}/status")
    public ResponseEntity<Map<String, Object>> updateDestinationStatus(@PathVariable Long id, @RequestParam boolean active) {
        Destination updated = destinationService.updateStatus(id, active);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Destination status updated successfully.");
        return ResponseEntity.ok(response);
    }

    // FIXED: Manual Cascade Delete for Destinations
    @DeleteMapping("/destinations/{id}")
    public ResponseEntity<Map<String, Object>> deleteDestination(@PathVariable Long id) {
        try {
            // MANUAL CASCADE: Delete linked packages (and their schedules) first
            List<SafariPackage> packages = safariPackageRepository.findAll();
            for (SafariPackage p : packages) {
                if (p.getDestination() != null && p.getDestination().getId().equals(id)) {

                    // Clear the schedules attached to this package
                    List<SafariSchedule> schedules = scheduleRepository.findAll();
                    for (SafariSchedule s : schedules) {
                        if (s.getSafariPackage() != null && s.getSafariPackage().getId().equals(p.getId())) {
                            scheduleRepository.deleteById(s.getId());
                        }
                    }
                    safariPackageRepository.deleteById(p.getId());
                }
            }

            // Finally, delete the destination itself bypassing SQL Server blocks
            destinationRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Destination deleted successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Database Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================= Schedules & Conflict Checking =================

    @GetMapping("/schedules")
    public ResponseEntity<List<SafariSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @GetMapping("/schedules/check-conflict")
    public ResponseEntity<Map<String, Object>> checkConflict(@RequestParam Long destinationId,
                                                             @RequestParam String date,
                                                             @RequestParam String timeSlot,
                                                             @RequestParam(required = false) Long excludeId) {
        LocalDate localDate = LocalDate.parse(date);
        boolean hasConflict = scheduleService.checkScheduleConflict(destinationId, localDate, timeSlot, excludeId);

        Map<String, Object> response = new HashMap<>();
        response.put("hasConflict", hasConflict);
        if (hasConflict) {
            response.put("message", "Schedule conflict detected: A schedule already exists for this destination, date, and slot.");
        } else {
            response.put("message", "Slot is available. No schedule conflicts detected.");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedules")
    public ResponseEntity<Map<String, Object>> createSchedule(@Valid @RequestBody ScheduleDto dto) {
        SafariSchedule schedule = scheduleService.createSchedule(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Safari schedule created successfully.");
        response.put("scheduleId", schedule.getId());
        response.put("date", schedule.getScheduleDate());
        response.put("timeSlot", schedule.getTimeSlot());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<Map<String, Object>> updateScheduleObj(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        // Same rules as creating a schedule: 1-40 guests, not below booked seats, no duplicate date/time slot
        ScheduleDto dto = new ScheduleDto();
        dto.setScheduleDate(payload.get("scheduleDate") != null && !payload.get("scheduleDate").toString().isBlank()
                ? LocalDate.parse(payload.get("scheduleDate").toString()) : null);
        dto.setTimeSlot(payload.get("timeSlot") != null ? payload.get("timeSlot").toString() : null);
        dto.setMaxSlots(payload.get("maxSlots") != null && !payload.get("maxSlots").toString().isBlank()
                ? Integer.valueOf(payload.get("maxSlots").toString().trim()) : null);
        dto.setStatus(null);   // editing must not reset the schedule's current status

        scheduleService.updateSchedule(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Schedule updated successfully.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Map<String, Object>> deleteScheduleObj(@PathVariable Long id) {
        scheduleRepository.deleteById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // ================= Safari Packages =================

    @PostMapping("/packages")
    public ResponseEntity<Map<String, Object>> createSafariPackage(@RequestBody Map<String, Object> payload) {
        // A package must have a title (blank or spaces-only titles are refused)
        String title = payload.get("title") != null ? payload.get("title").toString().trim() : "";
        String titleError = validatePackageTitle(title);
        if (titleError != null) {
            return packageBadRequest(titleError);
        }
        try {
            SafariPackage newPackage = new SafariPackage();
            newPackage.setTitle(title);

            String typeStr = payload.get("safariType").toString();
            newPackage.setSafariType(SafariType.valueOf(typeStr));

            newPackage.setDurationHours(Integer.parseInt(payload.get("durationHours").toString()));
            newPackage.setBasePrice(new java.math.BigDecimal(payload.get("basePrice").toString()));
            newPackage.setDescription(payload.get("description").toString());
            newPackage.setInclusions(payload.get("inclusions").toString());

            Long destId = Long.parseLong(payload.get("destinationId").toString());
            Destination dest = destinationService.getAllDestinations().stream()
                    .filter(d -> d.getId().equals(destId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Destination not found"));
            newPackage.setDestination(dest);

            safariPackageRepository.save(newPackage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Safari Package created successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error saving package: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ADDED: Completely missing Package Delete method!
    @DeleteMapping("/packages/{id}")
    public ResponseEntity<Map<String, Object>> deletePackageObj(@PathVariable Long id) {
        try {
            // MANUAL CASCADE: Bypass SQL Server constraints by deleting child schedules first
            List<SafariSchedule> schedules = scheduleRepository.findAll();
            for (SafariSchedule s : schedules) {
                if (s.getSafariPackage() != null && s.getSafariPackage().getId().equals(id)) {
                    scheduleRepository.deleteById(s.getId());
                }
            }

            // Once the children are clear, SQL Server will allow the parent package to be deleted
            safariPackageRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package deleted successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Database Error: " + e.getMessage());
            // Force a 500 error if it fails, so the frontend doesn't fake a success!
            return ResponseEntity.status(500).body(response);
        }
    }
    /** Returns an error message when the package title is missing or too long, otherwise null. */
    private static String validatePackageTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "Package title is required.";
        }
        if (title.length() < 3 || title.length() > 150) {
            return "Package title must be between 3 and 150 characters.";
        }
        return null;
    }

    private static ResponseEntity<Map<String, Object>> packageBadRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(400).body(response);
    }

    @PutMapping("/packages/{id}")
    public ResponseEntity<Map<String, Object>> updateSafariPackage(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            com.safari.wildlife.entity.SafariPackage pkg = packageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Package not found"));

            if (payload.containsKey("title")) {
                String title = payload.get("title") != null ? payload.get("title").toString().trim() : "";
                String titleError = validatePackageTitle(title);
                if (titleError != null) {
                    return packageBadRequest(titleError);
                }
                pkg.setTitle(title);
            }
            if (payload.get("description") != null) pkg.setDescription(payload.get("description").toString());
            if (payload.get("inclusions") != null) pkg.setInclusions(payload.get("inclusions").toString());
            if (payload.get("safariType") != null) pkg.setSafariType(com.safari.wildlife.enums.SafariType.valueOf(payload.get("safariType").toString()));
            if (payload.get("durationHours") != null) pkg.setDurationHours(Integer.parseInt(payload.get("durationHours").toString()));
            if (payload.get("basePrice") != null) pkg.setBasePrice(new java.math.BigDecimal(payload.get("basePrice").toString()));

            if (payload.get("destinationId") != null) {
                Long destId = Long.parseLong(payload.get("destinationId").toString());
                com.safari.wildlife.entity.Destination dest = destinationRepository.findById(destId).orElse(null);
                pkg.setDestination(dest);
            }

            packageRepository.save(pkg);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package updated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating package: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}