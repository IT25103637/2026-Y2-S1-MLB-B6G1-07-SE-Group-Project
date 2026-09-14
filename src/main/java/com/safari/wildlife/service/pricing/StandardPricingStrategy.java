package com.safari.wildlife.service.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Concrete Strategy: used outside the peak season (May - November). */
@Component("standardPricing")
public class StandardPricingStrategy implements PricingStrategy {

    private static final BigDecimal CHILD_RATE = new BigDecimal("0.50");

    @Override
    public String getName() {
        return "Standard pricing";
    }

    /**
     * Standard calculation: Full price for adults, 50% for children under 12, plus add-ons.
     */
    @Override
    public BigDecimal calculateTotal(BigDecimal basePrice, int adults, int children, BigDecimal addonsTotal) {
        BigDecimal adultTotal = basePrice.multiply(BigDecimal.valueOf(adults));
        BigDecimal childTotal = basePrice.multiply(CHILD_RATE).multiply(BigDecimal.valueOf(children));
        return adultTotal.add(childTotal).add(addonsTotal).setScale(2, RoundingMode.HALF_UP);
    }
}
