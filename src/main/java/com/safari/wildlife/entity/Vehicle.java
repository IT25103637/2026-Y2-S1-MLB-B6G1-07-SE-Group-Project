package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safari.wildlife.enums.VehicleStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity = 6;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "is_air_conditioned")
    private Boolean isAirConditioned = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ADDED: Cascade delete for Maintenance Records
    @JsonIgnore
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"vehicle", "hibernateLazyInitializer", "handler"})
    private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();

    // ADDED: Cascade delete for Vehicle Assignments (Bookings)
    @JsonIgnore
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"vehicle", "hibernateLazyInitializer", "handler"})
    private List<VehicleAssignment> vehicleAssignments = new ArrayList<>();

    public Vehicle() {}

    public Vehicle(String registrationNumber, String modelName, Integer seatingCapacity, Integer modelYear, Boolean isAirConditioned) {
        this.registrationNumber = registrationNumber;
        this.modelName = modelName;
        this.seatingCapacity = seatingCapacity;
        this.modelYear = modelYear;
        this.isAirConditioned = isAirConditioned;
        this.status = VehicleStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public Boolean getIsAirConditioned() {
        return isAirConditioned;
    }

    public void setIsAirConditioned(Boolean airConditioned) {
        isAirConditioned = airConditioned;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MaintenanceRecord> getMaintenanceRecords() {
        return maintenanceRecords;
    }

    public void setMaintenanceRecords(List<MaintenanceRecord> maintenanceRecords) {
        this.maintenanceRecords = maintenanceRecords;
    }

    public List<VehicleAssignment> getVehicleAssignments() {
        return vehicleAssignments;
    }

    public void setVehicleAssignments(List<VehicleAssignment> vehicleAssignments) {
        this.vehicleAssignments = vehicleAssignments;
    }
}