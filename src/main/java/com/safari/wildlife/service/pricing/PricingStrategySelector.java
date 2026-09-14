package com.safari.wildlife.service.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.EnumSet;
import java.util.Set;

/**
 * Strategy Pattern: chooses which pricing algorithm applies to a booking.
 * Sri Lanka's peak safari season runs from December to April; every other month uses standard pricing.
 * The frontend estimate in safari-app.js (PEAK_SEASON_MONTHS) uses the same months.
 */
@Component
public class PricingStrategySelector {

    private static final Set<Month> PEAK_MONTHS =
            EnumSet.of(Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL);

    private final PricingStrategy standardPricing;
    private final PricingStrategy peakPricing;

    public PricingStrategySelector(@Qualifier("standardPricing") PricingStrategy standardPricing,
                                   @Qualifier("peakPricing") PricingStrategy peakPricing) {
        this.standardPricing = standardPricing;
        this.peakPricing = peakPricing;
    }

    /** Returns the strategy for the safari date of a booking. */
    public PricingStrategy selectFor(LocalDate safariDate) {
        return isPeakSeason(safariDate) ? peakPricing : standardPricing;
    }

    public static boolean isPeakSeason(LocalDate safariDate) {
        return safariDate != null && PEAK_MONTHS.contains(safariDate.getMonth());
    }
}
