package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safari.wildlife.enums.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 50)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SafariSchedule schedule;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private SafariPackage safariPackage;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "guest_adults", nullable = false)
    private Integer guestAdults = 1;

    @Column(name = "guest_children", nullable = false)
    private Integer guestChildren = 0;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingCustomization> customizations = new ArrayList<>();

    // FIX 1: Added orphanRemoval = true to the Payment link
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"booking", "hibernateLazyInitializer", "handler"})
    private Payment payment;

    // FIX 2: Added the missing link to BookingApproval with cascading deletes
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"booking", "hibernateLazyInitializer", "handler"})
    private BookingApproval bookingApproval;

    // FIX 3: Cascading delete for Silva's Vehicle Assignments
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"booking", "hibernateLazyInitializer", "handler"})
    private VehicleAssignment vehicleAssignment;

    // FIX 4: Preemptive cascading delete for Guide Assignments
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"booking", "hibernateLazyInitializer", "handler"})
    private GuideAssignment guideAssignment;

    // FIX 5: Cascading delete for YOUR module (Support Tickets!)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"booking", "hibernateLazyInitializer", "handler"})
    private List<SupportTicket> supportTickets = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Booking() {}

    public Booking(String bookingReference, Customer customer, SafariSchedule schedule, SafariPackage safariPackage,
                   LocalDate bookingDate, Integer guestAdults, Integer guestChildren, BigDecimal totalAmount, String specialRequests) {
        this.bookingReference = bookingReference;
        this.customer = customer;
        this.schedule = schedule;
        this.safariPackage = safariPackage;
        this.bookingDate = bookingDate;
        this.guestAdults = guestAdults;
        this.guestChildren = guestChildren;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.PENDING;
        this.specialRequests = specialRequests;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public int getTotalGuests() {
        return (guestAdults != null ? guestAdults : 0) + (guestChildren != null ? guestChildren : 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public SafariSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(SafariSchedule schedule) {
        this.schedule = schedule;
    }

    public SafariPackage getSafariPackage() {
        return safariPackage;
    }

    public void setSafariPackage(SafariPackage safariPackage) {
        this.safariPackage = safariPackage;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Integer getGuestAdults() {
        return guestAdults;
    }

    public void setGuestAdults(Integer guestAdults) {
        this.guestAdults = guestAdults;
    }

    public Integer getGuestChildren() {
        return guestChildren;
    }

    public void setGuestChildren(Integer guestChildren) {
        this.guestChildren = guestChildren;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public List<BookingCustomization> getCustomizations() {
        return customizations;
    }

    public void setCustomizations(List<BookingCustomization> customizations) {
        this.customizations = customizations;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public BookingApproval getBookingApproval() {
        return bookingApproval;
    }

    public void setBookingApproval(BookingApproval bookingApproval) {
        this.bookingApproval = bookingApproval;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public VehicleAssignment getVehicleAssignment() {
        return vehicleAssignment;
    }

    public void setVehicleAssignment(VehicleAssignment vehicleAssignment) {
        this.vehicleAssignment = vehicleAssignment;
    }

    public GuideAssignment getGuideAssignment() {
        return guideAssignment;
    }

    public void setGuideAssignment(GuideAssignment guideAssignment) {
        this.guideAssignment = guideAssignment;
    }

    public List<SupportTicket> getSupportTickets() {
        return supportTickets;
    }

    public void setSupportTickets(List<SupportTicket> supportTickets) {
        this.supportTickets = supportTickets;
    }
}