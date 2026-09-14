package com.safari.wildlife.service.pricing;

import java.math.BigDecimal;

/**
 * DESIGN PATTERN: Strategy Pattern (Strategy role)
 * Justification: This interface defines a family of pricing algorithms.
 * Each algorithm lives in its own class (Standard vs. Peak Season) and BookingService (the Context)
 * only talks to this interface. PricingStrategySelector picks the algorithm for each booking at runtime,
 * so a new pricing rule is added as a new class without modifying BookingService.
 */
public interface PricingStrategy {

    /** Short label shown in the audit log, e.g. "Standard pricing". */
    String getName();

    /** Total for the guests plus the selected add-ons, rounded to cents. */
    BigDecimal calculateTotal(BigDecimal basePrice, int adults, int children, BigDecimal addonsTotal);
}
