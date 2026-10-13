package com.safari.wildlife;

import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.enums.PaymentStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.repository.*;
import com.safari.wildlife.service.AuthService;
import com.safari.wildlife.service.BookingService;
import com.safari.wildlife.service.PaymentService;
import com.safari.wildlife.service.observer.BookingEventPublisher;
import com.safari.wildlife.service.observer.BookingEventType;
import com.safari.wildlife.service.pricing.PricingStrategySelector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Customer "remove from history" and Finance Manager payment removal.
 * Plain JUnit + Mockito: no Spring context and no database.
 */
class PaymentRemovalTests {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentRemovalRepository removalRepository = mock(PaymentRemovalRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final List<BookingEventType> events = new ArrayList<>();
    private final BookingEventPublisher publisher = new BookingEventPublisher(List.of(e -> events.add(e.getType())));

    private final PaymentService paymentService = new PaymentService(paymentRepository, mock(PaymentReceiptRepository.class),
            mock(RefundRepository.class), bookingRepository, mock(SafariScheduleRepository.class), removalRepository, publisher);

    private final BookingService bookingService = new BookingService(bookingRepository, mock(SafariScheduleRepository.class),
            mock(SafariPackageRepository.class), mock(PackageOptionRepository.class), mock(CustomerRepository.class),
            mock(AuthService.class), mock(PricingStrategySelector.class), publisher);

    PaymentRemovalTests() {
        when(removalRepository.save(any(PaymentRemoval.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ------------------------------------------------------------------ Finance Manager removal

    @Test
    @DisplayName("Finance removal: a valid reason soft-deletes the payment and records ID, time, user and reason")
    void validRemovalIsRecorded() {
        Payment payment = payment(41L, PaymentStatus.PAID, "CARD");
        User finance = user(9L, "Finance Manager");

        PaymentRemoval removal = paymentService.removePayment(41L, finance, "  Duplicate test transaction entered during testing.  ");

        assertTrue(payment.isRemoved(), "payment is flagged as removed");
        verify(paymentRepository, never()).delete(any());
        verify(paymentRepository, never()).deleteById(any());
        assertEquals(41L, removal.getPayment().getId());
        assertSame(finance, removal.getRemovedBy());
        assertNotNull(removal.getRemovedAt());
        assertEquals("Duplicate test transaction entered during testing.", removal.getReason());
        assertEquals(payment.getTransactionRef(), removal.getTransactionRef());
        assertEquals(List.of(BookingEventType.PAYMENT_REMOVED), events, "audit observer is notified");
    }

    @Test
    @DisplayName("Finance removal: an empty, too short or meaningless reason is refused")
    void invalidReasonsAreRefused() {
        payment(42L, PaymentStatus.PAID, "CARD");
        User finance = user(9L, "Finance Manager");
        for (String bad : new String[]{null, "", "   ", "wrong", "1234567890!!", "aa........."}) {
            assertThrows(BadRequestException.class, () -> paymentService.removePayment(42L, finance, bad), "reason: " + bad);
        }
        verify(removalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Finance removal: a bank transfer awaiting verification and an already removed payment are refused")
    void pendingTransferAndRepeatRemovalAreRefused() {
        payment(43L, PaymentStatus.PENDING, "BANK_TRANSFER");
        User finance = user(9L, "Finance Manager");
        assertThrows(BadRequestException.class, () -> paymentService.removePayment(43L, finance, "Customer paid twice by mistake."));

        Payment removed = payment(44L, PaymentStatus.PAID, "CARD");
        removed.setRemoved(true);
        assertThrows(BadRequestException.class, () -> paymentService.removePayment(44L, finance, "Customer paid twice by mistake."));
        verify(removalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Finance ledger and summary leave out removed payments but keep all others")
    void ledgerExcludesOnlyRemovedPayments() {
        Payment kept = payment(51L, PaymentStatus.PAID, "CARD");
        Payment removed = payment(52L, PaymentStatus.PAID, "CARD");
        removed.setRemoved(true);
        when(paymentRepository.findAllByOrderByPaidAtDesc()).thenReturn(List.of(kept, removed));
        when(paymentRepository.findAll()).thenReturn(List.of(kept, removed));

        assertEquals(List.of(kept), paymentService.getAllPayments());
        assertEquals(1, paymentService.getFinancialSummary().get("paidTransactionsCount"));
    }

    // ------------------------------------------------------------------ Customer "remove from history"

    @Test
    @DisplayName("Customer remove: hides the booking from the customer only; booking and payment are kept")
    void customerRemoveOnlyHidesBooking() {
        User customer = user(5L, "Customer");
        Booking booking = booking(61L, customer, BookingStatus.CONFIRMED);
        Payment payment = new Payment(booking, "TXN-61", "CARD", new BigDecimal("110.00"));
        payment.setStatus(PaymentStatus.PAID);
        booking.setPayment(payment);

        bookingService.hideBookingForCustomer(61L, customer);

        assertTrue(booking.isHiddenByCustomer());
        assertSame(payment, booking.getPayment(), "payment link untouched");
        assertFalse(payment.isRemoved(), "finance record untouched");
        verify(bookingRepository, never()).delete(any());
        verify(bookingRepository, never()).deleteById(any());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Customer remove: another customer's booking and active bookings are refused")
    void customerRemoveRules() {
        User owner = user(5L, "Owner");
        User other = user(6L, "Other");
        booking(62L, owner, BookingStatus.CONFIRMED);
        assertThrows(BadRequestException.class, () -> bookingService.hideBookingForCustomer(62L, other));

        booking(63L, owner, BookingStatus.PENDING);
        booking(64L, owner, BookingStatus.APPROVED_AWAITING_PAYMENT);
        assertThrows(BadRequestException.class, () -> bookingService.hideBookingForCustomer(63L, owner));
        assertThrows(BadRequestException.class, () -> bookingService.hideBookingForCustomer(64L, owner));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Customer booking list leaves out only the bookings that customer removed")
    void customerListFiltersHidden() {
        User owner = user(5L, "Owner");
        Booking visible = booking(71L, owner, BookingStatus.CONFIRMED);
        Booking hidden = booking(72L, owner, BookingStatus.REFUNDED);
        hidden.hideForCustomer();
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(visible, hidden));

        assertEquals(List.of(visible), bookingService.getVisibleBookingsForCustomer(1L));
    }

    // ------------------------------------------------------------------ fixtures

    private User user(Long id, String name) {
        User u = new User(null, name, name.toLowerCase().replace(' ', '.') + "@safari.lk", "x", "0770000000");
        u.setId(id);
        return u;
    }

    private Booking booking(Long id, User owner, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setBookingReference("SLS-2026-" + id);
        b.setCustomer(new Customer(owner, "Sri Lankan", "200012345678", "Colombo", "0777654321"));
        b.setStatus(status);
        when(bookingRepository.findById(id)).thenReturn(Optional.of(b));
        return b;
    }

    private Payment payment(Long id, PaymentStatus status, String method) {
        Booking b = new Booking();
        b.setBookingReference("SLS-2026-P" + id);
        Payment p = new Payment(b, "TXN-" + id, method, new BigDecimal("110.00"));
        p.setId(id);
        p.setStatus(status);
        when(paymentRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }
}
