package com.safari.wildlife.controller;

import com.safari.wildlife.dto.BookingRequestDto;
import com.safari.wildlife.dto.PaymentRequestDto;
import com.safari.wildlife.dto.SupportTicketDto;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/customer")
public class CustomerApiController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final SupportTicketService ticketService;
    private final NotificationService notificationService;
    private final AuthService authService;

    @Autowired
    private com.safari.wildlife.repository.SupportTicketRepository ticketRepository;

    @Autowired
    private com.safari.wildlife.repository.BookingRepository bookingRepository;

    @Autowired
    private com.safari.wildlife.repository.SafariScheduleRepository scheduleRepository;

    // Added the PdfReceiptService here!
    @Autowired
    private com.safari.wildlife.service.PdfReceiptService pdfReceiptService;

    @Autowired
    public CustomerApiController(BookingService bookingService,
                                 PaymentService paymentService,
                                 SupportTicketService ticketService,
                                 NotificationService notificationService,
                                 AuthService authService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    // ================= Bookings =================

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(@Valid @RequestBody BookingRequestDto dto) {
        User user = authService.getCurrentUser();
        Booking booking = bookingService.createBooking(dto, user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking created successfully.");
        response.put("bookingReference", booking.getBookingReference());
        response.put("bookingId", booking.getId());
        response.put("status", booking.getStatus().name());
        response.put("totalAmount", booking.getTotalAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {
        Customer customer = authService.getCurrentCustomer();
        if (customer == null) {
            throw new BadRequestException("Customer profile not found.");
        }
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customer.getId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<Booking> getBookingDetails(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        Customer customer = authService.getCurrentCustomer();
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Access denied: You do not own this booking.");
        }
        return ResponseEntity.ok(booking);
    }

    // ================= Payment & Receipts =================

    @PostMapping("/bookings/{id}/pay")
    public ResponseEntity<Map<String, Object>> makePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto dto) {
        User user = authService.getCurrentUser();
        dto.setBookingId(id);
        Payment payment = paymentService.processPayment(dto, user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", payment.getStatus() == com.safari.wildlife.enums.PaymentStatus.PENDING
                ? "Bank transfer submitted. Your booking will be confirmed once our finance team verifies the payment."
                : "Payment processed successfully. Booking is now CONFIRMED!");
        response.put("paymentMethod", payment.getPaymentMethod());
        response.put("transactionRef", payment.getTransactionRef());
        response.put("amount", payment.getAmount());
        response.put("status", payment.getStatus().name());
        if (payment.getReceipt() != null) {
            response.put("receiptNumber", payment.getReceipt().getReceiptNumber());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentId}/receipt")
    public ResponseEntity<PaymentReceipt> getReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getReceiptByPaymentId(paymentId));
    }

    // NEW ENDPOINT: Triggers the direct PDF download in the browser
    @GetMapping("/bookings/{id}/receipt/download")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Generate the PDF
        byte[] pdfBytes = pdfReceiptService.generateReceipt(
                booking.getCustomer().getUser().getFullName(),
                booking.getBookingReference(),
                booking.getSafariPackage().getTitle(),
                booking.getTotalAmount().doubleValue()
        );

        // Tell the browser this is a downloadable PDF file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Receipt_" + booking.getBookingReference() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ================= Support Tickets =================

    @PostMapping("/support/tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@Valid @RequestBody SupportTicketDto dto) {
        User user = authService.getCurrentUser();
        SupportTicket ticket = ticketService.createTicket(dto, user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Support ticket submitted successfully.");
        response.put("ticketCode", ticket.getTicketCode());
        response.put("ticketId", ticket.getId());
        response.put("status", ticket.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/support/tickets")
    public ResponseEntity<List<SupportTicket>> getMyTickets() {
        Customer customer = authService.getCurrentCustomer();
        if (customer == null) {
            throw new BadRequestException("Customer profile not found.");
        }
        return ResponseEntity.ok(ticketService.getTicketsByCustomer(customer.getId()));
    }

    @GetMapping("/support/tickets/{id}")
    public ResponseEntity<SupportTicket> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // ================= Notifications =================

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelAndReleaseSlots(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Find the booking the customer wants to cancel
            Booking booking = bookingRepository.findById(id).orElse(null);

            if (booking != null) {
                // 2. Give the slots back to the Operations Manager's Calendar!
                com.safari.wildlife.entity.SafariSchedule schedule = booking.getSchedule();

                if (schedule != null) {
                    int adults = booking.getGuestAdults() != null ? booking.getGuestAdults() : 0;
                    int children = booking.getGuestChildren() != null ? booking.getGuestChildren() : 0;
                    int totalGuestsToRelease = adults + children;

                    int currentBooked = schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0;

                    // Subtract the guests, ensuring it never drops below 0
                    schedule.setBookedSlots(Math.max(0, currentBooked - totalGuestsToRelease));
                    scheduleRepository.save(schedule);
                }

                // 3. Finally, delete the booking to permanently clear it from the queue
                bookingRepository.delete(booking);
            }

            response.put("success", true);
            response.put("message", "Safari cancelled successfully and vehicle slots released.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error cancelling booking: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/support/tickets/{id}")
    public ResponseEntity<Map<String, Object>> updateTicketObj(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            com.safari.wildlife.entity.SupportTicket t = ticketRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            String catRaw = payload.get("category");
            if (catRaw != null && !catRaw.trim().isEmpty()) {
                String categoryStr = catRaw.toUpperCase().replace(" ", "_");
                t.setCategory(com.safari.wildlife.enums.TicketCategory.valueOf(categoryStr));
            }

            if (payload.get("subject") != null) {
                t.setSubject(payload.get("subject"));
            }
            if (payload.get("description") != null) {
                t.setDescription(payload.get("description"));
            }

            ticketRepository.save(t);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket updated successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Backend Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/support/tickets/{id}")
    public ResponseEntity<Map<String, Object>> deleteTicketObj(@PathVariable Long id) {
        try {
            ticketRepository.deleteById(id);
            Map<String, Object> response = new HashMap<>(); response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>(); response.put("success", false);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomerBooking(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        // Only the fields that were sent are changed; BookingService re-prices the booking with the pricing strategy
        BookingRequestDto dto = new BookingRequestDto();
        dto.setBookingDate(payload.get("bookingDate") != null && !payload.get("bookingDate").toString().isBlank()
                ? LocalDate.parse(payload.get("bookingDate").toString()) : null);
        dto.setGuestAdults(payload.get("guestAdults") != null ? Integer.parseInt(payload.get("guestAdults").toString()) : null);
        dto.setGuestChildren(payload.get("guestChildren") != null ? Integer.parseInt(payload.get("guestChildren").toString()) : null);
        dto.setSpecialRequests(payload.get("specialRequests") != null ? payload.get("specialRequests").toString() : null);

        Booking booking = bookingService.updateBooking(id, dto, authService.getCurrentUser());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking updated successfully.");
        response.put("totalAmount", booking.getTotalAmount());
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Map<String, Object>> deleteBookingHistory(@PathVariable Long id) {
        try {
            bookingService.deleteBookingHistory(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Booking removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}