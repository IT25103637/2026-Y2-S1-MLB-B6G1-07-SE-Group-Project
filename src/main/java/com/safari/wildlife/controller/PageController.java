package com.safari.wildlife.controller;

import com.safari.wildlife.entity.User;
import com.safari.wildlife.repository.DestinationRepository;
import com.safari.wildlife.repository.PackageOptionRepository;
import com.safari.wildlife.repository.SafariScheduleRepository;
import com.safari.wildlife.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {

    private final AuthService authService;
    private final DestinationRepository destinationRepository;
    private final SafariScheduleRepository scheduleRepository;
    private final PackageOptionRepository packageOptionRepository;

    @Autowired
    public PageController(AuthService authService,
                          DestinationRepository destinationRepository,
                          SafariScheduleRepository scheduleRepository,
                          PackageOptionRepository packageOptionRepository) {
        this.authService = authService;
        this.destinationRepository = destinationRepository;
        this.scheduleRepository = scheduleRepository;
        this.packageOptionRepository = packageOptionRepository;
    }

    // Prevents HTTP 500 error when the browser requests /favicon.ico
    @GetMapping("favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute("currentUser", user);

        // Injects the database rows into the template
        model.addAttribute("destinations", destinationRepository.findAll());
        model.addAttribute("schedules", scheduleRepository.findAll());
        model.addAttribute("packageOptions", packageOptionRepository.findAll());

        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "403";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        return "redirect:/#dashboard";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {
        return "redirect:/#customer-dashboard";
    }

    @GetMapping("/booking-manager/dashboard")
    public String bookingManagerDashboard() {
        return "redirect:/#manager-dashboard";
    }

    @GetMapping("/operations/dashboard")
    public String operationsDashboard() {
        return "redirect:/#operations-dashboard";
    }

    @GetMapping("/fleet/dashboard")
    public String fleetDashboard() {
        return "redirect:/#fleet-dashboard";
    }

    @GetMapping("/finance/dashboard")
    public String financeDashboard() {
        return "redirect:/#finance-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "redirect:/#admin-dashboard";
    }
}