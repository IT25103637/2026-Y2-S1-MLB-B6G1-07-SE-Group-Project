package com.safari.wildlife.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ScheduleDto {

    private Long id;

    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    @NotNull(message = "Safari package ID is required")
    private Long packageId;

    @NotNull(message = "Schedule date is required")
    private LocalDate scheduleDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot; // e.g., "MORNING_0600", "AFTERNOON_1430", "FULL_DAY_0600"

    @NotNull(message = "Max slots count is required")
    @Min(value = 1, message = "Max slots must be at least 1")
    private Integer maxSlots = 20;

    private String status = "AVAILABLE";

    public ScheduleDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
