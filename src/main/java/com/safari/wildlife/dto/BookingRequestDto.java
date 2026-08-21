package com.safari.wildlife.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingRequestDto {

    @NotNull(message = "Safari schedule ID is required")
    private Long scheduleId;

    @NotNull(message = "Safari package ID is required")
    private Long packageId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Number of adult guests is required")
    @Min(value = 1, message = "At least 1 adult guest is required")
    @Max(value = 10, message = "Maximum 10 adults per single booking")
    private Integer guestAdults = 1;

    @Min(value = 0, message = "Child count cannot be negative")
    @Max(value = 6, message = "Maximum 6 children per single booking")
    private Integer guestChildren = 0;

    private String specialRequests;

    private List<CustomizationItemDto> customizations = new ArrayList<>();

    public BookingRequestDto() {}

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
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

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public List<CustomizationItemDto> getCustomizations() {
        return customizations;
    }

    public void setCustomizations(List<CustomizationItemDto> customizations) {
        this.customizations = customizations;
    }
}
