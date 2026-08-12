package com.safari.wildlife.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_approvals")
public class BookingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(nullable = false, length = 30)
    private String action; // APPROVED, REJECTED

    @Column(name = "notes_or_reason", columnDefinition = "TEXT")
    private String notesOrReason;

    @Column(name = "action_date", updatable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    public BookingApproval() {}

    public BookingApproval(Booking booking, User manager, String action, String notesOrReason) {
        this.booking = booking;
        this.manager = manager;
        this.action = action;
        this.notesOrReason = notesOrReason;
        this.actionDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotesOrReason() {
        return notesOrReason;
    }

    public void setNotesOrReason(String notesOrReason) {
        this.notesOrReason = notesOrReason;
    }

    public LocalDateTime getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }
}
