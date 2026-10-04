package com.safari.wildlife;

import com.safari.wildlife.entity.Booking;
import com.safari.wildlife.entity.Customer;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.service.AuditService;
import com.safari.wildlife.service.EmailService;
import com.safari.wildlife.service.NotificationService;
import com.safari.wildlife.service.PdfReceiptService;
import com.safari.wildlife.service.observer.*;
import com.safari.wildlife.service.pricing.PeakSeasonPricingStrategy;
import com.safari.wildlife.service.pricing.PricingStrategy;
import com.safari.wildlife.service.pricing.PricingStrategySelector;
import com.safari.wildlife.service.pricing.StandardPricingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the two design patterns. Plain JUnit + Mockito: no Spring context and no database,
 * so they are safe to run at any time (unlike SafariWorkflowTests, which uses the real SQL Server database).
 */
class DesignPatternTests {

    private final PricingStrategy standard = new StandardPricingStrategy();
    private final PricingStrategy peak = new PeakSeasonPricingStrategy();
    private final PricingStrategySelector selector = new PricingStrategySelector(standard, peak);

    // ======================= Strategy Pattern =======================

    @Test
    @DisplayName("Strategy: standard pricing = adults full price + children half price + add-ons")
    void standardPricing() {
        // 2 adults x 120 = 240, 1 child x 60 = 60, add-ons 45
        assertEquals(new BigDecimal("345.00"), standard.calculateTotal(new BigDecimal("120.00"), 2, 1, new BigDecimal("45.00")));
    }

    @Test
    @DisplayName("Strategy: peak pricing adds 15% to the guest fares only, not to the add-ons")
    void peakPricing() {
        // (240 + 60) x 1.15 = 345, add-ons 45
        assertEquals(new BigDecimal("390.00"), peak.calculateTotal(new BigDecimal("120.00"), 2, 1, new BigDecimal("45.00")));
    }

    @Test
    @DisplayName("Strategy: peak pricing rounds to cents")
    void peakPricingRounding() {
        // 110 x 1 x 1.15 = 126.50; 133.33 x 1.15 = 153.3295 -> 153.33
        assertEquals(new BigDecimal("126.50"), peak.calculateTotal(new BigDecimal("110.00"), 1, 0, BigDecimal.ZERO));
        assertEquals(new BigDecimal("153.33"), peak.calculateTotal(new BigDecimal("133.33"), 1, 0, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Strategy: the selector picks Peak for December - April and Standard otherwise")
    void selectorPicksStrategyBySeason() {
        assertSame(standard, selector.selectFor(LocalDate.of(2026, 11, 30)));
        assertSame(peak, selector.selectFor(LocalDate.of(2026, 12, 1)));
        assertSame(peak, selector.selectFor(LocalDate.of(2027, 1, 15)));
        assertSame(peak, selector.selectFor(LocalDate.of(2027, 4, 30)));
        assertSame(standard, selector.selectFor(LocalDate.of(2027, 5, 1)));
        assertSame(standard, selector.selectFor(LocalDate.of(2027, 8, 10)));
        assertSame(standard, selector.selectFor(null));
    }

    // ======================= Observer Pattern =======================

    @Test
    @DisplayName("Observer: every attached observer is notified, in order; a detached one is not")
    void publisherNotifiesAttachedObservers() {
        List<String> calls = new ArrayList<>();
        BookingEventObserver first = e -> calls.add("first:" + e.getType());
        BookingEventObserver second = e -> calls.add("second:" + e.getType());

        BookingEventPublisher publisher = new BookingEventPublisher(List.of(first, second));
        publisher.attach(first);                       // attaching twice is ignored
        publisher.notifyObservers(new BookingEvent(BookingEventType.BOOKING_APPROVED, booking(), null));
        assertEquals(List.of("first:BOOKING_APPROVED", "second:BOOKING_APPROVED"), calls);

        calls.clear();
        publisher.detach(first);
        publisher.notifyObservers(new BookingEvent(BookingEventType.BOOKING_REJECTED, booking(), null));
        assertEquals(List.of("second:BOOKING_REJECTED"), calls);
    }

    @Test
    @DisplayName("Observer: approval -> customer notification, approval e-mail and audit log")
    void approvalReachesAllThreeObservers() {
        NotificationService notifications = mock(NotificationService.class);
        EmailService email = mock(EmailService.class);
        AuditService audit = mock(AuditService.class);
        User manager = new User(null, "Booking Manager", "booking@safari.lk", "x", "0770000000");

        BookingEventPublisher publisher = new BookingEventPublisher(List.of(
                new CustomerNotificationObserver(notifications),
                new CustomerEmailObserver(email, mock(PdfReceiptService.class)),
                new AuditLogObserver(audit)));

        Booking booking = booking();
        publisher.notifyObservers(new BookingEvent(BookingEventType.BOOKING_APPROVED, booking, manager));

        User customer = booking.getCustomer().getUser();
        verify(notifications).createNotification(eq(customer), eq("Booking Approved: SLS-2026-11111"), anyString(), eq("BOOKING"));
        verify(email).sendBookingApprovalEmail("customer@safari.lk", "Test Customer", "SLS-2026-11111");
        verify(audit).logAction(eq(manager), eq("BOOKING_APPROVE"), eq("Booking"), eq("SLS-2026-11111"),
                eq("Approved booking for customer: customer@safari.lk"), anyString());
    }

    @Test
    @DisplayName("Observer: a failing e-mail observer does not stop the approval or the audit log")
    void emailFailureDoesNotBreakOtherObservers() {
        EmailService email = mock(EmailService.class);
        doThrow(new RuntimeException("SMTP down")).when(email).sendBookingApprovalEmail(anyString(), anyString(), anyString());
        AuditService audit = mock(AuditService.class);

        BookingEventPublisher publisher = new BookingEventPublisher(List.of(
                new CustomerEmailObserver(email, mock(PdfReceiptService.class)),
                new AuditLogObserver(audit)));

        assertDoesNotThrow(() -> publisher.notifyObservers(new BookingEvent(BookingEventType.BOOKING_APPROVED, booking(), null)));
        verify(audit).logAction(isNull(), eq("BOOKING_APPROVE"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Observer: rejection reason reaches the customer notification and the audit log")
    void rejectionCarriesReason() {
        NotificationService notifications = mock(NotificationService.class);
        AuditService audit = mock(AuditService.class);
        BookingEventPublisher publisher = new BookingEventPublisher(List.of(
                new CustomerNotificationObserver(notifications), new AuditLogObserver(audit)));

        publisher.notifyObservers(new BookingEvent(BookingEventType.BOOKING_REJECTED, booking(), null).withDetail("No vehicles"));

        verify(notifications).createNotification(any(), eq("Booking Rejected: SLS-2026-11111"),
                eq("We regret to inform you that your booking could not be approved. Reason: No vehicles"), eq("BOOKING"));
        verify(audit).logAction(any(), eq("BOOKING_REJECT"), eq("Booking"), eq("SLS-2026-11111"),
                eq("Rejected booking. Reason: No vehicles"), anyString());
    }

    private static Booking booking() {
        User customerUser = new User(null, "Test Customer", "customer@safari.lk", "x", "0771234567");
        Customer customer = new Customer(customerUser, "Sri Lankan", "200012345678", "Colombo", "0777654321");
        Booking booking = mock(Booking.class);
        when(booking.getBookingReference()).thenReturn("SLS-2026-11111");
        when(booking.getCustomer()).thenReturn(customer);
        return booking;
    }
}
