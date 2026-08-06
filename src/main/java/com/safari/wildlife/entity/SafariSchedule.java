package com.safari.wildlife.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "safari_schedules", uniqueConstraints = {
    @UniqueConstraint(name = "uq_dest_schedule", columnNames = {"destination_id", "schedule_date", "time_slot"})
})
public class SafariSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private SafariPackage safariPackage;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "time_slot", nullable = false, length = 50)
    private String timeSlot; // e.g. "MORNING_0600", "AFTERNOON_1430", "FULL_DAY_0600"

    @Column(name = "max_slots", nullable = false)
    private Integer maxSlots = 20;

    @Column(name = "booked_slots", nullable = false)
    private Integer bookedSlots = 0;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE, FULL, CANCELLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Cascade delete so removing a schedule removes its linked test bookings
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.OneToMany(mappedBy = "schedule", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Booking> bookings = new java.util.ArrayList<>();

    public SafariSchedule() {}

    public SafariSchedule(Destination destination, SafariPackage safariPackage, LocalDate scheduleDate, String timeSlot, Integer maxSlots) {
        this.destination = destination;
        this.safariPackage = safariPackage;
        this.scheduleDate = scheduleDate;
        this.timeSlot = timeSlot;
        this.maxSlots = maxSlots;
        this.bookedSlots = 0;
        this.status = "AVAILABLE";
        this.createdAt = LocalDateTime.now();
    }

    public boolean hasAvailableSlots(int requestedSlots) {
        return (this.bookedSlots + requestedSlots) <= this.maxSlots;
    }

    public void addSlots(int slots) {
        this.bookedSlots += slots;
        if (this.bookedSlots >= this.maxSlots) {
            this.status = "FULL";
        }
    }

    public void releaseSlots(int slots) {
        this.bookedSlots = Math.max(0, this.bookedSlots - slots);
        if (this.bookedSlots < this.maxSlots && !"CANCELLED".equals(this.status)) {
            this.status = "AVAILABLE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public SafariPackage getSafariPackage() {
        return safariPackage;
    }

    public void setSafariPackage(SafariPackage safariPackage) {
        this.safariPackage = safariPackage;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Integer getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(Integer maxSlots) {
        this.maxSlots = maxSlots;
    }

    public Integer getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(Integer bookedSlots) {
        this.bookedSlots = bookedSlots;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
