package com.safari.wildlife.controller;

import com.safari.wildlife.dto.RefundRequestDto;
import com.safari.wildlife.repository.PaymentRepository;
import com.safari.wildlife.entity.Payment;
import com.safari.wildlife.entity.PaymentReceipt;
import com.safari.wildlife.entity.Refund;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.service.AuthService;
import com.safari.wildlife.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
public class FinanceApiController {

    private final PaymentService paymentService;
    private final AuthService authService;

    @Autowired
    public FinanceApiController(PaymentService paymentService, AuthService authService) {
        this.paymentService = paymentService;
        this.authService = authService;
    }

    @Autowired
    private com.safari.wildlife.repository.PaymentRepository paymentRepository;

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/receipts/{paymentId}")
    public ResponseEntity<PaymentReceipt> getReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getReceiptByPaymentId(paymentId));
    }

    @PostMapping("/refunds")
    public ResponseEntity<Map<String, Object>> processRefund(@Valid @RequestBody RefundRequestDto dto) {
        User manager = authService.getCurrentUser();
        Refund refund = paymentService.processRefund(dto, manager);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Refund of $" + refund.getAmount() + " processed successfully. Reference: " + refund.getRefundRef());
        response.put("refundRef", refund.getRefundRef());
        response.put("refundId", refund.getId());
        return ResponseEntity.ok(response);
    }

    // ================= Bank transfer verification =================

    @PostMapping("/payments/{id}/verify-transfer")
    public ResponseEntity<Map<String, Object>> verifyBankTransfer(@PathVariable Long id) {
        Payment payment = paymentService.verifyBankTransfer(id, authService.getCurrentUser());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bank transfer verified. Booking " + payment.getBooking().getBookingReference() + " is now CONFIRMED.");
        if (payment.getReceipt() != null) {
            response.put("receiptNumber", payment.getReceipt().getReceiptNumber());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{id}/reject-transfer")
    public ResponseEntity<Map<String, Object>> rejectBankTransfer(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Payment payment = paymentService.rejectBankTransfer(id, authService.getCurrentUser(), body.get("reason"));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bank transfer rejected. The customer has been notified and can pay again.");
        response.put("status", payment.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary() {
        return ResponseEntity.ok(paymentService.getFinancialSummary());
    }

    @GetMapping("/report/csv")
    public ResponseEntity<byte[]> generateCsvReport() {
        // 1. Fetch all financial records
        List<Payment> payments = paymentRepository.findAll();

        // 2. Build the CSV string
        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Booking Reference,Amount (USD),Payment Method,Status,Transaction Date\n");

        for (Payment p : payments) {
            String bookingRef = p.getBooking() != null ? p.getBooking().getBookingReference() : "N/A";

            // Corrected getters based on your Payment entity
            String date = p.getPaidAt() != null ? p.getPaidAt().toString() : "N/A";

            // Format each row cleanly (Corrected getTransactionRef)
            csv.append(String.format("%s,%s,%.2f,%s,%s,%s\n",
                    p.getTransactionRef(),
                    bookingRef,
                    p.getAmount(),
                    p.getPaymentMethod(),
                    p.getStatus(),
                    date
            ));
        }

        // 3. Convert to bytes and set up HTTP headers for a file download
        byte[] csvBytes = csv.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "financial_report.csv");

        // 4. Return the file
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Map<String, Object>> deletePaymentObj(@PathVariable Long id) {
        try {
            paymentRepository.deleteById(id);
            Map<String, Object> response = new HashMap<>(); response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>(); response.put("success", false);
            return ResponseEntity.status(500).body(response);
        }
    }
}
