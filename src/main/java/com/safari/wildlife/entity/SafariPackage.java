package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safari.wildlife.enums.SafariType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "safari_packages")
public class SafariPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prevents recursion when serializing SafariPackage -> Destination -> packages
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    @JsonIgnoreProperties({"packages", "hibernateLazyInitializer", "handler"})
    private Destination destination;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "safari_type", nullable = false, length = 50)
    private SafariType safariType;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity = 6;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String inclusions;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Prevents recursion when serializing SafariPackage -> PackageOption -> safariPackage
    @OneToMany(mappedBy = "safariPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"safariPackage", "hibernateLazyInitializer", "handler"})
    private List<PackageOption> options = new ArrayList<>();

    // Tell SQL Server to cascade deletes to attached schedules and bookings
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.OneToMany(mappedBy = "safariPackage", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SafariSchedule> schedules = new java.util.ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.OneToMany(mappedBy = "safariPackage", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Booking> bookings = new java.util.ArrayList<>();

    public SafariPackage() {}

    public SafariPackage(Destination destination, String title, SafariType safariType, BigDecimal basePrice, Integer durationHours, Integer maxCapacity, String description, String inclusions) {
        this.destination = destination;
        this.title = title;
        this.safariType = safariType;
        this.basePrice = basePrice;
        this.durationHours = durationHours;
        this.maxCapacity = maxCapacity;
        this.description = description;
        this.inclusions = inclusions;
        this.isActive = true;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SafariType getSafariType() {
        return safariType;
    }

    public void setSafariType(SafariType safariType) {
        this.safariType = safariType;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInclusions() {
        return inclusions;
    }

    public void setInclusions(String inclusions) {
        this.inclusions = inclusions;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<PackageOption> getOptions() {
        return options;
    }

    public void setOptions(List<PackageOption> options) {
        this.options = options;
    }
}