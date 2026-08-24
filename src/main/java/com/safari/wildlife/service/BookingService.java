package com.safari.wildlife.service;

import com.safari.wildlife.dto.BookingRequestDto;
import com.safari.wildlife.dto.CustomizationItemDto;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.service.observer.BookingEvent;
import com.safari.wildlife.service.observer.BookingEventPublisher;
import com.safari.wildlife.service.observer.BookingEventType;
import com.safari.wildlife.service.pricing.PricingStrategy;
import com.safari.wildlife.service.pricing.PricingStrategySelector;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SafariScheduleRepository scheduleRepository;
    private final SafariPackageRepository packageRepository;
    private final PackageOptionRepository optionRepository;
    private final CustomerRepository customerRepository;
    private final AuthService authService;
    private final SecureRandom random = new SecureRandom();

    // DESIGN PATTERN: Strategy Pattern - picks Standard or Peak Season pricing for each booking date
    private final PricingStrategySelector pricingSelector;

    // DESIGN PATTERN: Observer Pattern - notification, e-mail and audit observers react to booking events
    private final BookingEventPublisher bookingEvents;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          SafariScheduleRepository scheduleRepository,
                          SafariPackageRepository packageRepository,
                          PackageOptionRepository optionRepository,
                          CustomerRepository customerRepository,
                          AuthService authService,
                          PricingStrategySelector pricingSelector,
                          BookingEventPublisher bookingEvents) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.packageRepository = packageRepository;
        this.optionRepository = optionRepository;
        this.customerRepository = customerRepository;
        this.authService = authService;
        this.pricingSelector = pricingSelector;
        this.bookingEvents = bookingEvents;
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Booking getBookingByReference(String reference) {
        return bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));
    }

    @Transactional
    public Booking createBooking(BookingRequestDto dto, User currentUser) {
        if (dto.getGuestAdults() == null || dto.getGuestAdults() < 1) {
            throw new BadRequestException("At least 1 adult guest is required for booking.");
        }
        if (dto.getBookingDate() == null) {
            throw new BadRequestException("Booking date is required.");
        }

        Customer customer = customerRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Customer profile not found for user: " + currentUser.getEmail()));

        SafariSchedule schedule = scheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Safari schedule not found with id: " + dto.getScheduleId()));

        SafariPackage safariPackage = packageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Safari package not found with id: " + dto.getPackageId()));

        // 1. Calculate total guests
        int totalGuests = dto.getGuestAdults() + (dto.getGuestChildren() != null ? dto.getGuestChildren() : 0);

        // 2. CHECK capacity BEFORE modifying any numbers
        if (!schedule.hasAvailableSlots(totalGuests)) {
            throw new BadRequestException("This safari trip is no longer available. Only "
                    + (schedule.getMaxSlots() - schedule.getBookedSlots()) + " slots remain.");
        }

        if (totalGuests > safariPackage.getMaxCapacity() * 2) {
            throw new BadRequestException("Group size exceeds maximum permitted vehicle limit for this package.");
        }

        Booking booking = new Booking(
                generateBookingReference(),
                customer,
                schedule,
                safariPackage,
                dto.getBookingDate(),
                dto.getGuestAdults(),
                dto.getGuestChildren() != null ? dto.getGuestChildren() : 0,
                BigDecimal.ZERO,
                dto.getSpecialRequests()
        );

        BigDecimal customizationTotal = BigDecimal.ZERO;
        List<BookingCustomization> customizations = new ArrayList<>();

        if (dto.getCustomizations() != null && !dto.getCustomizations().isEmpty()) {
            for (CustomizationItemDto item : dto.getCustomizations()) {
                if (item.getOptionId() != null) {
                    PackageOption option = optionRepository.findById(item.getOptionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Package option not found with id: " + item.getOptionId()));
                    int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                    BookingCustomization custom = new BookingCustomization(booking, option, qty, option.getPrice());
                    customizations.add(custom);
                    customizationTotal = customizationTotal.add(custom.getTotalPrice());
                }
            }
        }

        // APPLY THE STRATEGY PATTERN FOR PRICING CALCULATION
        // The selector returns the Standard or Peak Season strategy for the safari date (peak = December - April)
        int adults = dto.getGuestAdults();
        int children = dto.getGuestChildren() != null ? dto.getGuestChildren() : 0;
        PricingStrategy pricing = pricingSelector.selectFor(dto.getBookingDate());
        BigDecimal finalTotal = pricing.calculateTotal(safariPackage.getBasePrice(), adults, children, customizationTotal);

        booking.setTotalAmount(finalTotal);
        booking.setCustomizations(customizations);

        // 3. Safely update the schedule capacity explicitly!
        int currentBooked = schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0;
        schedule.setBookedSlots(currentBooked + totalGuests);
        scheduleRepository.save(schedule);

        Booking saved = bookingRepository.save(booking);

        // Observer Pattern: announce the event; the observers send the notification and write the audit log
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BOOKING_CREATED, saved, currentUser)
                .withDetail(pricing.getName()));

        return saved;
    }

    /**
     * Customer edits a booking that is still PENDING review: date, guests and special requests.
     * Fields left null keep their current value. The price is recalculated with the pricing strategy
     * for the (possibly new) safari date, and the add-ons the customer chose are kept.
     */
    @Transactional
    public Booking updateBooking(Long id, BookingRequestDto dto, User currentUser) {
        Booking booking = getBookingById(id);

        if (!booking.getCustomer().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to edit this booking.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only bookings that are still PENDING review can be edited. Current status: " + booking.getStatus());
        }

        int adults = dto.getGuestAdults() != null ? dto.getGuestAdults() : booking.getGuestAdults();
        int children = dto.getGuestChildren() != null ? dto.getGuestChildren()
                : (booking.getGuestChildren() != null ? booking.getGuestChildren() : 0);
        if (adults < 1) {
            throw new BadRequestException("At least 1 adult guest is required for booking.");
        }
        if (children < 0) {
            throw new BadRequestException("Number of children cannot be negative.");
        }
        int oldGuests = booking.getTotalGuests();
        int newGuests = adults + children;
        if (newGuests > booking.getSafariPackage().getMaxCapacity() * 2) {
            throw new BadRequestException("Group size exceeds maximum permitted vehicle limit for this package.");
        }

        SafariSchedule schedule = booking.getSchedule();
        int diff = newGuests - oldGuests;
        if (diff > 0 && !schedule.hasAvailableSlots(diff)) {
            throw new BadRequestException("Additional slots not available on this schedule.");
        }

        if (diff != 0) {
            schedule.releaseSlots(oldGuests);
            schedule.addSlots(newGuests);
            scheduleRepository.save(schedule);
        }

        if (dto.getBookingDate() != null) {
            booking.setBookingDate(dto.getBookingDate());
        }
        booking.setGuestAdults(adults);
        booking.setGuestChildren(children);
        if (dto.getSpecialRequests() != null) {
            booking.setSpecialRequests(dto.getSpecialRequests());
        }

        // Recalculate using Strategy Pattern (keeps the add-ons the customer already chose)
        BigDecimal addonsTotal = BigDecimal.ZERO;
        for (BookingCustomization c : booking.getCustomizations()) {
            addonsTotal = addonsTotal.add(c.getTotalPrice());
        }
        PricingStrategy pricing = pricingSelector.selectFor(booking.getBookingDate());
        booking.setTotalAmount(pricing.calculateTotal(booking.getSafariPackage().getBasePrice(), adults, children, addonsTotal));

        Booking updated = bookingRepository.save(booking);

        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BOOKING_UPDATED, updated, currentUser)
                .withDetail(pricing.getName()));

        return updated;
    }

    @Transactional
    public Booking cancelCustomerBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        if (!booking.getCustomer().getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("Unauthorized: You do not have permission to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state. Please contact support for refunds.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        if (booking.getSchedule() != null) {
            int totalGuests = booking.getGuestAdults() + (booking.getGuestChildren() != null ? booking.getGuestChildren() : 0);
            int currentBooked = booking.getSchedule().getBookedSlots() != null ? booking.getSchedule().getBookedSlots() : 0;

            booking.getSchedule().setBookedSlots(Math.max(0, currentBooked - totalGuests));
            scheduleRepository.save(booking.getSchedule());
        }

        return bookingRepository.save(booking);
    }
    public void deleteBookingHistory(Long id) {
        // Hard deletes the record from the database to clean up the UI
        bookingRepository.deleteById(id);
    }

    private String generateBookingReference() {
        int rand = 10000 + random.nextInt(90000);
        return "SLS-2026-" + rand;
    }
}