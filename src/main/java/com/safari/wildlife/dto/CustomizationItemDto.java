package com.safari.wildlife.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CustomizationItemDto {

    @NotNull(message = "Package option ID is required")
    private Long optionId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    public CustomizationItemDto() {}

    public CustomizationItemDto(Long optionId, Integer quantity) {
        this.optionId = optionId;
        this.quantity = quantity;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
