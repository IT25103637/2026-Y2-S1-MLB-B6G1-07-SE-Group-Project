package com.safari.wildlife.controller;

import com.safari.wildlife.dto.LoginRequest;
import com.safari.wildlife.dto.RegisterRequest;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.repository.UserRepository;
import com.safari.wildlife.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.registerCustomer(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Customer account registered successfully. You can now log in.");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);
        httpRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        User user = authService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole().getName().name());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        User user = authService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("authenticated", true);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("phone", user.getPhone() != null ? user.getPhone() : "");
            response.put("role", user.getRole().getName().name());
        } else {
            response.put("authenticated", false);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestParam String email, @RequestParam String newPassword) {
        authService.resetPassword(email, newPassword);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password has been successfully reset. Please log in with your new credentials.");
        return ResponseEntity.ok(response);
    }

    // Paste this INSIDE your AuthController class body:

    @Autowired
    private UserRepository userRepository;

    // "My Profile" updates are handled by UserProfileController (PUT /api/user/profile).
    // The method that used to be here was mapped to /api/auth/api/user/profile because of this
    // class's @RequestMapping("/api/auth") prefix, so the page's requests never reached it.

    @GetMapping("/api/auth/current-user")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        // If no one is logged in, safely return false
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        try {
            // Fetch the user from the database using their session email
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Safely get the role string
            String roleName = user.getRole() != null ? user.getRole().toString() : "ROLE_CUSTOMER";

            // Return the user data to the frontend
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "role", roleName
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }
}
