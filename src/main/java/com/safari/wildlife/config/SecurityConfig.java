package com.safari.wildlife.config;

import com.safari.wildlife.entity.User;
import com.safari.wildlife.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByEmail(username.trim().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

            if (!user.isActive()) {
                throw new UsernameNotFoundException("User account is deactivated.");
            }

            // Ensure the authority always has the "ROLE_" prefix expected by .hasRole(...)
            String roleStr = user.getRole().getName().name();
            String authorityName = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority(authorityName))
            );
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access Denied: You do not have permission to access this resource.\"}");
            } else {
                response.sendRedirect("/access-denied");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabled for REST API / SPA AJAX compatibility
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Public views and resources
                        .requestMatchers(
                                "/", "/index.html", "/login", "/register", "/forgot-password", "/access-denied",
                                "/css/**", "/js/**", "/images/**", "/videos/**", "/models/**", "/favicon.ico",
                                "/h2-console/**",
                                "/api/auth/**",
                                "/api/public/**",
                                "/api/destinations", "/api/destinations/**",
                                "/api/packages", "/api/packages/**"
                        ).permitAll()

                        // Customer routes
                        .requestMatchers("/customer/**", "/api/customer/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        // Booking Manager & Fleet Manager routes (Fleet needs access to bookings for vehicle assignment)
                        .requestMatchers("/booking-manager/**", "/api/manager/**", "/api/booking-manager/**")
                        .hasAnyRole("BOOKING_MANAGER", "FLEET_MANAGER", "ADMIN")

                        // Operations Manager routes
                        .requestMatchers("/operations/**", "/api/operations/**")
                        .hasAnyRole("OPERATIONS_MANAGER", "ADMIN")

                        // Fleet Manager routes
                        .requestMatchers("/fleet/**", "/api/fleet/**")
                        .hasAnyRole("FLEET_MANAGER", "ADMIN")

                        // Finance Manager routes
                        .requestMatchers("/finance/**", "/api/finance/**")
                        .hasAnyRole("FINANCE_MANAGER", "ADMIN")

                        // Admin routes
                        .requestMatchers("/admin/**", "/api/admin/**")
                        .hasRole("ADMIN")

                        // Authenticated general endpoints
                        .requestMatchers("/dashboard", "/api/notifications/**").authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/#dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("SAFARI_SESSION_ID", "JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}