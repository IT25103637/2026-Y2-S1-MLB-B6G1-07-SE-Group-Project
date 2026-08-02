package com.safari.wildlife.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entry_rules", columnDefinition = "TEXT")
    private String entryRules;

    @Column(name = "opening_time", length = 20)
    private String openingTime;

    @Column(name = "closing_time", length = 20)
    private String closingTime;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Tell SQL Server to cascade deletes to attached packages and schedules
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.OneToMany(mappedBy = "destination", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SafariPackage> packages = new java.util.ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.OneToMany(mappedBy = "destination", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SafariSchedule> schedules = new java.util.ArrayList<>();

    public Destination() {}

    public Destination(String name, String province, String description, String entryRules, String openingTime, String closingTime, String imageUrl) {
        this.name = name;
        this.province = province;
        this.description = description;
        this.entryRules = entryRules;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.imageUrl = imageUrl;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntryRules() {
        return entryRules;
    }

    public void setEntryRules(String entryRules) {
        this.entryRules = entryRules;
    }

    public String getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(String openingTime) {
        this.openingTime = openingTime;
    }

    public String getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(String closingTime) {
        this.closingTime = closingTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
