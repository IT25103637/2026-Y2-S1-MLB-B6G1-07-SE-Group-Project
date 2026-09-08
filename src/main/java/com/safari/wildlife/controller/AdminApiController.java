package com.safari.wildlife.controller;

import com.safari.wildlife.dto.UserManagementDto;
import com.safari.wildlife.entity.AuditLog;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.enums.UserRole;
import com.safari.wildlife.service.AdminService;
import com.safari.wildlife.service.AuditService;
import com.safari.wildlife.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.safari.wildlife.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.safari.wildlife.repository.RoleRepository roleRepository;

    private final AdminService adminService;
    private final AuditService auditService;
    private final AuthService authService;

    @Autowired
    public AdminApiController(AdminService adminService, AuditService auditService, AuthService authService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.authService = authService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserManagementDto dto) {
        User admin = authService.getCurrentUser();
        User user = adminService.createUser(dto, admin);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User " + user.getEmail() + " created successfully with role " + user.getRole().getName());
        response.put("userId", user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id, @RequestParam UserRole role) {
        User admin = authService.getCurrentUser();
        User user = adminService.updateUserRole(id, role, admin);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Role updated to " + role + " for user " + user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id, @RequestParam boolean active) {
        User admin = authService.getCurrentUser();
        User user = adminService.toggleUserStatus(id, active, admin);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User status updated to active=" + active + " for " + user.getEmail());
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> updateUser(@org.springframework.web.bind.annotation.PathVariable Long id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        try {
            // Find the user
            com.safari.wildlife.entity.User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update Name and Phone
            user.setFullName(payload.get("fullName"));
            user.setPhone(payload.get("phone"));

            // Find and Update Role
            String roleName = payload.get("role");

            // FIX: Convert the Enum to a String before comparing it!
            if (roleName != null && !roleName.isEmpty() && user.getRole() != null &&
                    !user.getRole().getName().toString().equals("ROLE_CUSTOMER")) {

                com.safari.wildlife.entity.Role role = roleRepository.findByName(com.safari.wildlife.enums.UserRole.valueOf(roleName))
                        .orElseThrow(() -> new RuntimeException("Role not found"));
                user.setRole(role);
            }

            userRepository.save(user);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully.");
            return org.springframework.http.ResponseEntity.ok(response);

        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating user: " + e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }
}
