package com.safari.wildlife.service;

import com.safari.wildlife.dto.UserManagementDto;
import com.safari.wildlife.entity.Role;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.enums.UserRole;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.RoleRepository;
import com.safari.wildlife.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Autowired
    public AdminService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        AuditService auditService,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User createUser(UserManagementDto dto, User adminUser) {
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required.");
        }
        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("A user with email '" + dto.getEmail().trim() + "' already exists.");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        Role role = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + dto.getRole()));

        User user = new User(
                role,
                dto.getFullName().trim(),
                dto.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(dto.getPassword().trim()),
                dto.getPhone()
        );
        user.setActive(dto.getActive() != null ? dto.getActive() : true);

        User saved = userRepository.save(user);

        notificationService.createNotification(
                saved,
                "Account Created by Administrator",
                "Your staff account has been set up with role: " + role.getName() + ". You may now log in.",
                "SYSTEM"
        );

        auditService.logAction(adminUser, "ADMIN_CREATE_USER", "User",
                String.valueOf(saved.getId()), "Admin created user " + saved.getEmail() + " with role " + role.getName(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public User updateUserRole(Long userId, UserRole newRole, User adminUser) {
        User user = getUserById(userId);
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRole));

        user.setRole(role);
        User updated = userRepository.save(user);

        auditService.logAction(adminUser, "ADMIN_CHANGE_ROLE", "User",
                String.valueOf(user.getId()), "Changed role of " + user.getEmail() + " to " + newRole, "127.0.0.1");

        return updated;
    }

    @Transactional
    public User toggleUserStatus(Long userId, boolean active, User adminUser) {
        User user = getUserById(userId);
        user.setActive(active);
        User updated = userRepository.save(user);

        auditService.logAction(adminUser, "ADMIN_USER_STATUS", "User",
                String.valueOf(user.getId()), "Set user active=" + active + " for " + user.getEmail(), "127.0.0.1");

        return updated;
    }
}
