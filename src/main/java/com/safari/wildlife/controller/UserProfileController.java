package com.safari.wildlife.controller;

import com.safari.wildlife.entity.User;
import com.safari.wildlife.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * "My Profile" for every signed-in user (customers and all staff roles).
 * /api/user/** is not in any role-specific rule, so SecurityConfig's anyRequest().authenticated() applies.
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> payload) {
        User user = authService.updateProfile(payload.get("fullName"), payload.get("phone"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully.");
        response.put("fullName", user.getFullName());
        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
        return ResponseEntity.ok(response);
    }
}
