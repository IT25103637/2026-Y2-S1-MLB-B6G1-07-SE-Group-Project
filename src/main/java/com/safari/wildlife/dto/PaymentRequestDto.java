package com.safari.wildlife.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payment request for every supported method. Which fields are required depends on
 * paymentMethod and is validated in PaymentService:
 *   CARD          -> cardHolderName, cardNumber, expiryDate, cvv
 *   LANKAQR       -> qrReference (optional app reference)
 *   EZCASH/MCASH/GENIE -> mobileNumber, otp
 *   BANK_TRANSFER -> bankName, transferReference
 */
public class PaymentRequestDto {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    // ---- card
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;

    private String paymentMethod = "CARD";

    // ---- LankaQR
    private String qrReference;

    // ---- mobile wallets
    private String mobileNumber;
    private String otp;

    // ---- bank transfer
    private String bankName;
    private String transferReference;

    // Testing / grading flag to verify Scenario 6 payment failure & retry
    private boolean simulateFailure = false;

    public PaymentRequestDto() {}

    public String getQrReference() {
        return qrReference;
    }

    public void setQrReference(String qrReference) {
        this.qrReference = qrReference;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setTransferReference(String transferReference) {
        this.transferReference = transferReference;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
