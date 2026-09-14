package com.safari.wildlife.service.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Concrete Strategy: used in Sri Lanka's peak safari season (December - April). */
@Component("peakPricing")
public class PeakSeasonPricingStrategy implements PricingStrategy {

    private static final BigDecimal CHILD_RATE = new BigDecimal("0.50");
    private static final BigDecimal PEAK_SURCHARGE = new BigDecimal("1.15");

    @Override
    public String getName() {
        return "Peak season pricing (+15%)";
    }

    /**
     * Peak Season calculation: Applies a 15% surcharge to the guest fares. Add-ons keep their normal price.
     */
    @Override
    public BigDecimal calculateTotal(BigDecimal basePrice, int adults, int children, BigDecimal addonsTotal) {
        BigDecimal adultTotal = basePrice.multiply(BigDecimal.valueOf(adults));
        BigDecimal childTotal = basePrice.multiply(CHILD_RATE).multiply(BigDecimal.valueOf(children));
        BigDecimal guestTotalWithSurcharge = adultTotal.add(childTotal).multiply(PEAK_SURCHARGE); // 15% Peak Surcharge

        return guestTotalWithSurcharge.add(addonsTotal).setScale(2, RoundingMode.HALF_UP);
    }
}
