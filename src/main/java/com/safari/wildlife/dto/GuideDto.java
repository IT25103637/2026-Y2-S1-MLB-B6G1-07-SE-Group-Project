package com.safari.wildlife.dto;

import com.safari.wildlife.enums.GuideStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class GuideDto {

    private Long id;

    @NotBlank(message = "Guide full name is required")
    private String fullName;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String languagesSpoken;
    private String phone;

    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears = 1;

    private GuideStatus status = GuideStatus.AVAILABLE;

    public GuideDto() {}

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
