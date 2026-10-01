package com.safari.wildlife.service;

import com.safari.wildlife.dto.SupportTicketDto;
import com.safari.wildlife.dto.TicketResponseDto;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.TicketStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class SupportTicketService {

    @Autowired
    private com.safari.wildlife.service.EmailService emailService;

    private final SupportTicketRepository ticketRepository;
    private final TicketResponseRepository responseRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SupportTicketService(SupportTicketRepository ticketRepository,
                                TicketResponseRepository responseRepository,
                                CustomerRepository customerRepository,
                                BookingRepository bookingRepository,
                                NotificationService notificationService,
                                AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.responseRepository = responseRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getTicketsByCustomer(Long customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with id: " + id));
    }

    @Transactional
    public SupportTicket createTicket(SupportTicketDto dto, User customerUser) {
        Customer customer = customerRepository.findByUser(customerUser)
                .orElseThrow(() -> new BadRequestException("Customer profile not found for user: " + customerUser.getEmail()));

        Booking booking = null;
        if (dto.getBookingId() != null) {
            booking = bookingRepository.findById(dto.getBookingId()).orElse(null);
        }

        String ticketCode = "TCK-" + (1000 + random.nextInt(9000));
        SupportTicket ticket = new SupportTicket(
                ticketCode,
                customer,
                booking,
                dto.getCategory(),
                dto.getSubject().trim(),
                dto.getDescription().trim(),
                dto.getPriority()
        );

        SupportTicket saved = ticketRepository.save(ticket);

        notificationService.createNotification(
                customerUser,
                "Support Ticket Created: " + saved.getTicketCode(),
                "Your request has been received. Our Customer Relations Manager will respond shortly.",
                "SUPPORT"
        );

        auditService.logAction(customerUser, "TICKET_CREATE", "SupportTicket",
                saved.getTicketCode(), "Customer created support ticket: " + saved.getSubject(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public TicketResponse respondToTicket(Long ticketId, TicketResponseDto dto, User staffUser) {
        SupportTicket ticket = getTicketById(ticketId);

        if (dto.getResponseText() == null || dto.getResponseText().trim().isEmpty()) {
            throw new BadRequestException("Response text is required.");
        }

        TicketResponse response = new TicketResponse(ticket, staffUser, dto.getResponseText().trim());
        TicketResponse savedResponse = responseRepository.save(response);

        // Update ticket status (e.g. IN_PROGRESS, RESOLVED, or REJECTED)
        if (dto.getNewStatus() != null) {
            ticket.setStatus(dto.getNewStatus());
        } else {
            ticket.setStatus(TicketStatus.RESOLVED);
        }
        ticketRepository.save(ticket);

        // Notify customer in-app
        User customerUser = ticket.getCustomer().getUser();
        notificationService.createNotification(
                customerUser,
                "Update on Support Ticket: " + ticket.getTicketCode(),
                "Our team responded to your ticket. Status updated to: " + ticket.getStatus(),
                "SUPPORT"
        );

        // ==========================================
        // TRIGGER EMAIL NOTIFICATION
        // ==========================================
        emailService.sendSupportReplyEmail(
                customerUser.getEmail(),
                customerUser.getFullName(),
                ticket.getTicketCode(),
                dto.getResponseText().trim()
        );

        auditService.logAction(staffUser, "TICKET_RESPOND", "SupportTicket",
                ticket.getTicketCode(), "Manager responded to ticket. New status: " + ticket.getStatus(), "127.0.0.1");

        return savedResponse;
    }
}