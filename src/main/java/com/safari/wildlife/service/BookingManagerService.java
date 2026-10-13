package com.safari.wildlife.service;

import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.*;
import com.safari.wildlife.service.observer.BookingEvent;
import com.safari.wildlife.service.observer.BookingEventPublisher;
import com.safari.wildlife.service.observer.BookingEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingManagerService {

    private final BookingRepository bookingRepository;
    private final BookingApprovalRepository approvalRepository;
    private final SafariScheduleRepository scheduleRepository;
    // DESIGN PATTERN: Observer Pattern - the customer is notified, e-mailed and the action audited by observers
    private final BookingEventPublisher bookingEvents;

    @Autowired
    public BookingManagerService(BookingRepository bookingRepository,
                                 BookingApprovalRepository approvalRepository,
                                 SafariScheduleRepository scheduleRepository,
                                 BookingEventPublisher bookingEvents) {
        this.bookingRepository = bookingRepository;
        this.approvalRepository = approvalRepository;
        this.scheduleRepository = scheduleRepository;
        this.bookingEvents = bookingEvents;
    }
    //crud
    @Transactional(readOnly = true)
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING);
    }

    @Transactional
    public Booking approveBooking(Long bookingId, User manager, String notes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking cannot be approved because current status is: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.APPROVED_AWAITING_PAYMENT);
        Booking updated = bookingRepository.save(booking);

        BookingApproval approval = new BookingApproval(booking, manager, "APPROVED", notes != null ? notes : "Approved by Booking Manager");
        approvalRepository.save(approval);

        // Observer Pattern: notification + approval e-mail + audit log are handled by the observers
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BOOKING_APPROVED, updated, manager));

        return updated;
    }

    @Transactional
    public Booking rejectBooking(Long bookingId, User manager, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("A rejection reason is mandatory when rejecting a booking.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking cannot be rejected because current status is: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.REJECTED);

        // Release reserved schedule slots
        SafariSchedule schedule = booking.getSchedule();
        schedule.releaseSlots(booking.getTotalGuests());
        scheduleRepository.save(schedule);

        Booking updated = bookingRepository.save(booking);

        BookingApproval approval = new BookingApproval(booking, manager, "REJECTED", rejectionReason.trim());
        approvalRepository.save(approval);

        // Observer Pattern: notification + audit log are handled by the observers
        bookingEvents.notifyObservers(new BookingEvent(BookingEventType.BOOKING_REJECTED, updated, manager)
                .withDetail(rejectionReason.trim()));

        return updated;
    }
}
