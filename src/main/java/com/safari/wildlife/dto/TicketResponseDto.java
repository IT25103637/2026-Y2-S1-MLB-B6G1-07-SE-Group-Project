package com.safari.wildlife.dto;

import com.safari.wildlife.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;

public class TicketResponseDto {

    @NotBlank(message = "Response text cannot be blank")
    private String responseText;

    private TicketStatus newStatus = TicketStatus.RESOLVED;

    public TicketResponseDto() {}

    public TicketResponseDto(String responseText, TicketStatus newStatus) {
        this.responseText = responseText;
        this.newStatus = newStatus;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public TicketStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TicketStatus newStatus) {
        this.newStatus = newStatus;
    }
}
