package com.safari.wildlife.controller;

import com.safari.wildlife.dto.TicketResponseDto;
import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.SupportTicket;
import com.safari.wildlife.entity.TicketResponse;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.service.AuthService;
import com.safari.wildlife.service.BookingManagerService;
import com.safari.wildlife.service.BookingService;
import com.safari.wildlife.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
public class BookingManagerApiController {

    private final BookingManagerService bookingManagerService;
    private final BookingService bookingService;
    private final SupportTicketService ticketService;
    private final AuthService authService;

    @Autowired
    private com.safari.wildlife.repository.BookingRepository bookingRepository;

    // FIX: Added the SupportTicketRepository so the controller can delete tickets directly!
    @Autowired
    private com.safari.wildlife.repository.SupportTicketRepository supportTicketRepository;

    @Autowired
    public BookingManagerApiController(BookingManagerService bookingManagerService,
                                       BookingService bookingService,
                                       SupportTicketService ticketService,
                                       AuthService authService) {
        this.bookingManagerService = bookingManagerService;
        this.bookingService = bookingService;
        this.ticketService = ticketService;
        this.authService = authService;
    }

    // ================= Bookings Approval/Rejection =================

    @GetMapping("/bookings/pending")
    public ResponseEntity<List<Booking>> getPendingBookings() {
        return ResponseEntity.ok(bookingManagerService.getPendingBookings());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PostMapping("/bookings/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveBooking(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        User manager = authService.getCurrentUser();
        String notes = body != null ? body.get("notes") : null;

        // Approves the booking; the approval e-mail is now sent by CustomerEmailObserver (Observer Pattern)
        Booking booking = bookingManagerService.approveBooking(id, manager, notes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking " + booking.getBookingReference() + " approved. Customer notified to complete payment.");
        response.put("status", booking.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectBooking(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User manager = authService.getCurrentUser();
        String reason = body != null ? body.get("reason") : null;
        Booking booking = bookingManagerService.rejectBooking(id, manager, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking " + booking.getBookingReference() + " rejected. Schedule slots released.");
        response.put("status", booking.getStatus().name());
        return ResponseEntity.ok(response);
    }

    // ================= Customer Inquiries & Support Tickets =================

    @GetMapping("/support/tickets")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PostMapping("/support/tickets/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToTicket(@PathVariable Long id, @Valid @RequestBody TicketResponseDto dto) {
        User manager = authService.getCurrentUser();
        TicketResponse responseObj = ticketService.respondToTicket(id, dto, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Response posted to ticket. Customer notified.");
        response.put("newStatus", responseObj.getTicket().getStatus().name());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Map<String, Object>> deleteBookingObj(@PathVariable Long id) {
        // Deleting a booking would cascade to its payment, receipt and refunds; finance records must be kept
        com.safari.wildlife.entity.Booking existing = bookingRepository.findById(id).orElse(null);
        if (existing != null && existing.getPayment() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "This booking has payment records and cannot be deleted.");
            return ResponseEntity.status(400).body(response);
        }
        try {
            bookingRepository.deleteById(id);
            Map<String, Object> response = new HashMap<>(); response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>(); response.put("success", false);
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/support/tickets/{id}")
    public ResponseEntity<?> dismissSupportTicket(@PathVariable Long id) {
        try {
            // Find the ticket and delete it using the newly injected repository
            supportTicketRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket successfully dismissed.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Database Error: Cannot delete ticket. " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}