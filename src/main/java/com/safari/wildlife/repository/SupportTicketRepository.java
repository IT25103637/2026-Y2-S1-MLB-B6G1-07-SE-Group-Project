package com.safari.wildlife.repository;

import com.safari.wildlife.entity.SupportTicket;
import com.safari.wildlife.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketCode(String ticketCode);
    List<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(TicketStatus status);
}
