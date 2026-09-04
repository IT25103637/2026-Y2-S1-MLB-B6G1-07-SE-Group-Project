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
