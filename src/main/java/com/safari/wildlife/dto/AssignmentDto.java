package com.safari.wildlife.dto;

import jakarta.validation.constraints.NotNull;

public class AssignmentDto {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private Long vehicleId;
    private Long guideId;

    public AssignmentDto() {}

    public AssignmentDto(Long bookingId, Long vehicleId, Long guideId) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.guideId = guideId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }
}
