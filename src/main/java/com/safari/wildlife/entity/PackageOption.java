package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "package_options")
public class PackageOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonIgnore stops the circular JSON serialization loop
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private SafariPackage safariPackage;

    @Column(name = "option_name", nullable = false, length = 150)
    private String optionName;

    @Column(name = "option_description", length = 255)
    private String optionDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public PackageOption() {}

    public PackageOption(SafariPackage safariPackage, String optionName, String optionDescription, BigDecimal price) {
        this.safariPackage = safariPackage;
        this.optionName = optionName;
        this.optionDescription = optionDescription;
        this.price = price;
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SafariPackage getSafariPackage() {
        return safariPackage;
    }

    public void setSafariPackage(SafariPackage safariPackage) {
        this.safariPackage = safariPackage;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public String getOptionDescription() {
        return optionDescription;
    }

    public void setOptionDescription(String optionDescription) {
        this.optionDescription = optionDescription;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}