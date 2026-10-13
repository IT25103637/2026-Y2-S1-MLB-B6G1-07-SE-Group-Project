package com.safari.wildlife.controller;

import com.safari.wildlife.dto.AssignmentDto;
import com.safari.wildlife.dto.GuideDto;
import com.safari.wildlife.dto.MaintenanceDto;
import com.safari.wildlife.enums.VehicleStatus;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.service.AuthService;
import com.safari.wildlife.service.FleetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fleet")
public class FleetApiController {

    private final FleetService fleetService;
    private final AuthService authService;

    @Autowired
    private com.safari.wildlife.repository.VehicleRepository vehicleRepository;

    @Autowired
    private com.safari.wildlife.repository.GuideRepository guideRepository;

    @Autowired
    public FleetApiController(FleetService fleetService, AuthService authService) {
        this.fleetService = fleetService;
        this.authService = authService;
    }

    // ================= Vehicles =================

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(fleetService.getAllVehicles());
    }

    @PutMapping("/vehicles/{id}/status")
    public ResponseEntity<Map<String, Object>> updateVehicleStatus(@PathVariable Long id, @RequestParam String status) {
        fleetService.updateVehicleStatus(id, VehicleStatus.valueOf(status));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle status updated successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> createVehicle(@RequestBody Map<String, Object> payload) {
        // BACKEND SECURITY CHECK: Prevent 0 or negative seats
        Integer capacity = parseIntOrNull(payload.get("seatingCapacity"));
        if (capacity == null || capacity < 1 || capacity > 12) {
            return badRequest("Vehicle capacity must be between 1 and 12 seats.");
        }

        com.safari.wildlife.dto.VehicleDto dto = new com.safari.wildlife.dto.VehicleDto();
        dto.setRegistrationNumber(payload.get("registrationNumber") != null ? payload.get("registrationNumber").toString() : null);
        dto.setModelName(payload.get("modelName") != null ? payload.get("modelName").toString() : null);
        dto.setModelYear(parseIntOrNull(payload.get("modelYear")));
        dto.setSeatingCapacity(capacity);
        dto.setAirConditioned(payload.get("isAirConditioned") != null && Boolean.parseBoolean(payload.get("isAirConditioned").toString()));
        dto.setStatus(com.safari.wildlife.enums.VehicleStatus.AVAILABLE);

        // FleetService checks the registration number (required, valid, not a duplicate) and the model name
        com.safari.wildlife.entity.Vehicle saved = fleetService.registerVehicle(dto, authService.getCurrentUser());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle added successfully.");
        response.put("vehicleId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // --- MISSING VEHICLE UPDATE AND DELETE ENDPOINTS ADDED HERE ---
    @PutMapping("/vehicles/{id}")
    public ResponseEntity<Map<String, Object>> updateVehicleObj(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        com.safari.wildlife.entity.Vehicle v = fleetService.getVehicleById(id);

        // Fields that are not sent keep their current value
        Integer capacity = v.getSeatingCapacity();
        if (payload.get("seatingCapacity") != null && !payload.get("seatingCapacity").toString().isEmpty()) {
            // BACKEND SECURITY CHECK: Prevent 0 or negative seats on update
            capacity = parseIntOrNull(payload.get("seatingCapacity"));
            if (capacity == null || capacity < 1 || capacity > 12) {
                return badRequest("Vehicle capacity must be between 1 and 12 seats.");
            }
        }

        com.safari.wildlife.dto.VehicleDto dto = new com.safari.wildlife.dto.VehicleDto();
        dto.setRegistrationNumber(payload.containsKey("registrationNumber")
                ? (payload.get("registrationNumber") != null ? payload.get("registrationNumber").toString() : null)
                : v.getRegistrationNumber());
        dto.setModelName(payload.containsKey("modelName")
                ? (payload.get("modelName") != null ? payload.get("modelName").toString() : null)
                : v.getModelName());
        Integer year = parseIntOrNull(payload.get("modelYear"));
        dto.setModelYear(year != null ? year : v.getModelYear());
        dto.setSeatingCapacity(capacity);
        dto.setAirConditioned(payload.get("isAirConditioned") != null
                ? Boolean.parseBoolean(payload.get("isAirConditioned").toString()) : v.getIsAirConditioned());
        dto.setStatus(null);   // editing details must not change the vehicle's status (e.g. under maintenance)

        // FleetService checks the registration number (required, valid, not used by another vehicle)
        fleetService.updateVehicle(id, dto, authService.getCurrentUser());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle updated successfully.");
        return ResponseEntity.ok(response);
    }

    private static Integer parseIntOrNull(Object value) {
        if (value == null || value.toString().trim().isEmpty()) return null;
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(400).body(response);
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Map<String, Object>> deleteVehicleObj(@PathVariable Long id) {
        try {
            vehicleRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vehicle deleted successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Cannot delete: Vehicle is assigned to an active trip.");
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================= Maintenance =================

    @GetMapping("/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<List<MaintenanceRecord>> getVehicleMaintenance(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(fleetService.getVehicleMaintenance(vehicleId));
    }

    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, Object>> recordMaintenance(@Valid @RequestBody MaintenanceDto dto) {
        User manager = authService.getCurrentUser();
        MaintenanceRecord record = fleetService.recordMaintenance(dto, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Maintenance record created. Vehicle " + record.getVehicle().getRegistrationNumber() +
                " is now UNDER MAINTENANCE and locked from trip assignments.");
        response.put("maintenanceId", record.getId());
        response.put("vehicleStatus", record.getVehicle().getStatus().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/maintenance/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeMaintenance(@PathVariable Long id) {
        User manager = authService.getCurrentUser();
        MaintenanceRecord record = fleetService.completeMaintenance(id, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Maintenance marked as completed. Vehicle restored to AVAILABLE status.");
        response.put("vehicleStatus", record.getVehicle().getStatus().name());
        return ResponseEntity.ok(response);
    }


    // ================= Guides =================

    @GetMapping("/guides")
    public ResponseEntity<List<Guide>> getAllGuides() {
        return ResponseEntity.ok(fleetService.getAllGuides());
    }

    @PostMapping("/guides")
    public ResponseEntity<Map<String, Object>> registerGuide(@Valid @RequestBody GuideDto dto) {
        User manager = authService.getCurrentUser();
        Guide guide = fleetService.registerGuide(dto, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Guide " + guide.getFullName() + " registered successfully.");
        response.put("guideId", guide.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/guides/{id}/status")
    public ResponseEntity<Map<String, Object>> updateGuideStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            // 1. Find the Guide directly
            com.safari.wildlife.entity.Guide guide = guideRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Guide not found"));

            // 2. Safely map the status from the frontend to the Java Enum
            com.safari.wildlife.enums.GuideStatus newStatus;
            try {
                newStatus = com.safari.wildlife.enums.GuideStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // If "UNAVAILABLE" doesn't exist in your Enum, fallback to "ON_LEAVE"
                newStatus = com.safari.wildlife.enums.GuideStatus.valueOf("ON_LEAVE");
            }

            // 3. Save it
            guide.setStatus(newStatus);
            guideRepository.save(guide);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Guide status updated successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Server Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/guides/{id}")
    public ResponseEntity<Map<String, Object>> updateGuide(@PathVariable Long id, @Valid @RequestBody GuideDto dto) {
        User manager = authService.getCurrentUser();
        fleetService.updateGuide(id, dto, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Guide updated successfully.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/guides/{id}")
    public ResponseEntity<Map<String, Object>> deleteGuide(@PathVariable Long id) {
        fleetService.deleteGuide(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Guide deleted successfully.");
        return ResponseEntity.ok(response);
    }

    // ================= Dispatch & Assignments =================

    @PostMapping("/assign-vehicle")
    public ResponseEntity<Map<String, Object>> assignVehicle(@Valid @RequestBody AssignmentDto dto) {
        User manager = authService.getCurrentUser();
        VehicleAssignment assignment = fleetService.assignVehicleToBooking(dto.getBookingId(), dto.getVehicleId(), manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle " + assignment.getVehicle().getRegistrationNumber() +
                " assigned successfully to booking " + assignment.getBooking().getBookingReference());
        response.put("assignmentId", assignment.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-guide")
    public ResponseEntity<Map<String, Object>> assignGuide(@Valid @RequestBody AssignmentDto dto) {
        User manager = authService.getCurrentUser();
        GuideAssignment assignment = fleetService.assignGuideToBooking(dto.getBookingId(), dto.getGuideId(), manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Guide " + assignment.getGuide().getFullName() +
                " assigned successfully to booking " + assignment.getBooking().getBookingReference());
        response.put("assignmentId", assignment.getId());
        return ResponseEntity.ok(response);
    }
}