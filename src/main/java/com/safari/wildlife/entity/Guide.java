package com.safari.wildlife.entity;

import com.safari.wildlife.enums.GuideStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "guides")
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "languages_spoken", length = 200)
    private String languagesSpoken;

    @Column(length = 30)
    private String phone;

    @Column(name = "experience_years")
    private Integer experienceYears = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuideStatus status = GuideStatus.AVAILABLE;

    public Guide() {}

    public Guide(String fullName, String licenseNumber, String languagesSpoken, String phone, Integer experienceYears) {
        this.fullName = fullName;
        this.licenseNumber = licenseNumber;
        this.languagesSpoken = languagesSpoken;
        this.phone = phone;
        this.experienceYears = experienceYears;
        this.status = GuideStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLanguagesSpoken() {
        return languagesSpoken;
    }

    public void setLanguagesSpoken(String languagesSpoken) {
        this.languagesSpoken = languagesSpoken;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public GuideStatus getStatus() {
        return status;
    }

    public void setStatus(GuideStatus status) {
        this.status = status;
    }
}
