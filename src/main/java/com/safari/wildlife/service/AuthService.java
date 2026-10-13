package com.safari.wildlife.service;

import com.safari.wildlife.dto.RegisterRequest;
import com.safari.wildlife.entity.Customer;
import com.safari.wildlife.entity.Role;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.enums.UserRole;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.CustomerRepository;
import com.safari.wildlife.repository.RoleRepository;
import com.safari.wildlife.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       NotificationService notificationService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public User registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists.");
        }

        Role customerRole = roleRepository.findByName(UserRole.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found in system."));

        User user = new User(
                customerRole,
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone()
        );
        User savedUser = userRepository.save(user);

        Customer customer = new Customer(
                savedUser,
                request.getNationality(),
                request.getPassportOrNic(),
                request.getAddress(),
                request.getEmergencyContact()
        );
        customerRepository.save(customer);

        notificationService.createNotification(
                savedUser,
                "Welcome to Sri Lanka Wild-Life Safaris!",
                "Your customer account has been created successfully. Explore our national park safaris and book your adventure.",
                "SYSTEM"
        );

        auditService.logAction(savedUser, "USER_REGISTER", "User", String.valueOf(savedUser.getId()),
                "Customer self-registered with email " + savedUser.getEmail(), "127.0.0.1");

        return savedUser;
    }

    /** "My Profile": the signed-in user updates their own full name and phone number. */
    @Transactional
    public User updateProfile(String fullName, String phone) {
        User user = getCurrentUser();
        if (user == null) {
            throw new BadRequestException("Please sign in to update your profile.");
        }
        String name = fullName == null ? "" : fullName.trim();
        if (name.length() < 2 || name.length() > 100) {
            throw new BadRequestException("Full name must be between 2 and 100 characters.");
        }
        String tel = phone == null ? "" : phone.trim();
        if (!tel.isEmpty()) {
            String digits = tel.replaceAll("\\D", "");
            if (!tel.matches("\\+?[0-9 ()-]+") || digits.length() < 9 || digits.length() > 15) {
                throw new BadRequestException("Please enter a valid phone number, e.g. +94 77 123 4567.");
            }
        }

        user.setFullName(name);
        user.setPhone(tel.isEmpty() ? null : tel);
        User saved = userRepository.save(user);

        auditService.logAction(saved, "PROFILE_UPDATE", "User", saved.getEmail(),
                "Updated own profile (name / phone)", "127.0.0.1");
        return saved;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @Transactional(readOnly = true)
    public Customer getCurrentCustomer() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return customerRepository.findByUser(user).orElse(null);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        notificationService.createNotification(
                user,
                "Password Reset Successful",
                "Your account password was updated successfully. If this was not you, contact staff immediately.",
                "SYSTEM"
        );
    }
}
