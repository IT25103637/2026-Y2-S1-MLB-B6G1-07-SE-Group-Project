package com.safari.wildlife;

import com.safari.wildlife.dto.*;
import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.BookingStatus;
import com.safari.wildlife.enums.PaymentStatus;
import com.safari.wildlife.enums.TicketStatus;
import com.safari.wildlife.enums.UserRole;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.PaymentProcessingException;
import com.safari.wildlife.exception.ScheduleConflictException;
import com.safari.wildlife.exception.VehicleUnavailableException;
import com.safari.wildlife.repository.*;
import com.safari.wildlife.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SafariWorkflowTests {

    @Autowired private BookingService bookingService;
    @Autowired private BookingManagerService bookingManagerService;
    @Autowired private DestinationService destinationService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private FleetService fleetService;
    @Autowired private PaymentService paymentService;
    @Autowired private SupportTicketService ticketService;
    @Autowired private AdminService adminService;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SafariScheduleRepository scheduleRepository;
    @Autowired private SafariPackageRepository packageRepository;
    @Autowired private PackageOptionRepository optionRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private MockMvc mockMvc;

    // Helper to get seeded demo customer
    private User getCustomerUser() {
        return userRepository.findByEmail("customer@safari.lk").orElseThrow();
    }

    // Helper to get seeded booking manager
    private User getBookingManagerUser() {
        return userRepository.findByEmail("booking@safari.lk").orElseThrow();
    }

    // Helper to get seeded operations manager
    private User getOperationsManagerUser() {
        return userRepository.findByEmail("operations@safari.lk").orElseThrow();
    }

    // Helper to get seeded fleet manager
    private User getFleetManagerUser() {
        return userRepository.findByEmail("fleet@safari.lk").orElseThrow();
    }

    // Helper to get seeded finance manager
    private User getFinanceManagerUser() {
        return userRepository.findByEmail("finance@safari.lk").orElseThrow();
    }

    // Helper to get seeded admin
    private User getAdminUser() {
        return userRepository.findByEmail("admin@safari.lk").orElseThrow();
    }

    @Test
    @Order(1)
    @DisplayName("Scenario 1: Customer searches -> selects trip -> customizes package -> creates booking")
    void testScenario1_CustomerCreatesBookingWithCustomization() {
        User customer = getCustomerUser();
        SafariSchedule schedule = scheduleRepository.findAll().get(0);
        SafariPackage pkg = packageRepository.findAll().get(0);
        PackageOption option = optionRepository.findBySafariPackageId(pkg.getId()).get(0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setScheduleId(schedule.getId());
        dto.setPackageId(pkg.getId());
        dto.setBookingDate(schedule.getScheduleDate());
        dto.setGuestAdults(2);
        dto.setGuestChildren(1);
        dto.setSpecialRequests("First time safari visitors. Prefer morning departure.");

        CustomizationItemDto customDto = new CustomizationItemDto(option.getId(), 1);
        dto.setCustomizations(List.of(customDto));

        int slotsBefore = schedule.getBookedSlots();
        Booking booking = bookingService.createBooking(dto, customer);

        assertNotNull(booking);
        assertNotNull(booking.getBookingReference());
        assertTrue(booking.getBookingReference().startsWith("SLS-"));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(2, booking.getGuestAdults());
        assertEquals(1, booking.getGuestChildren());
        assertTrue(booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // Verify schedule capacity updated
        SafariSchedule updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals(slotsBefore + 3, updatedSchedule.getBookedSlots());
    }

    @Test
    @Order(2)
    @DisplayName("Scenario 2: Invalid booking details -> system rejects/asks for correction")
    void testScenario2_InvalidBookingDetailsRejected() {
        User customer = getCustomerUser();
        SafariSchedule schedule = scheduleRepository.findAll().get(0);
        SafariPackage pkg = packageRepository.findAll().get(0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setScheduleId(schedule.getId());
        dto.setPackageId(pkg.getId());
        dto.setBookingDate(schedule.getScheduleDate());
        dto.setGuestAdults(0); // Invalid: 0 adults

        assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(dto, customer);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Scenario 3: Booking Manager reviews -> approves booking")
    void testScenario3_BookingManagerApprovesBooking() {
        User manager = getBookingManagerUser();
        List<Booking> pending = bookingManagerService.getPendingBookings();
        assertFalse(pending.isEmpty(), "Pending bookings should exist");

        Booking target = pending.get(0);
        Booking approved = bookingManagerService.approveBooking(target.getId(), manager, "All identity checks verified.");

        assertEquals(BookingStatus.APPROVED_AWAITING_PAYMENT, approved.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Scenario 4: Booking Manager rejects booking with reason")
    void testScenario4_BookingManagerRejectsBooking() {
        User customer = getCustomerUser();
        User manager = getBookingManagerUser();
        SafariSchedule schedule = scheduleRepository.findAll().get(0);
        SafariPackage pkg = packageRepository.findAll().get(0);

        // Create a temporary booking to reject
        BookingRequestDto dto = new BookingRequestDto();
        dto.setScheduleId(schedule.getId());
        dto.setPackageId(pkg.getId());
        dto.setBookingDate(schedule.getScheduleDate());
        dto.setGuestAdults(2);
        Booking tempBooking = bookingService.createBooking(dto, customer);

        int slotsBefore = scheduleRepository.findById(schedule.getId()).orElseThrow().getBookedSlots();

        // Reject without reason fails
        assertThrows(BadRequestException.class, () -> {
            bookingManagerService.rejectBooking(tempBooking.getId(), manager, "");
        });

        // Reject with reason succeeds
        Booking rejected = bookingManagerService.rejectBooking(tempBooking.getId(), manager, "Park capacity restriction.");
        assertEquals(BookingStatus.REJECTED, rejected.getStatus());

        // Verify slots were safely released back
        int slotsAfter = scheduleRepository.findById(schedule.getId()).orElseThrow().getBookedSlots();
        assertEquals(slotsBefore - 2, slotsAfter);
    }

    @Test
    @Order(5)
    @DisplayName("Scenario 5: Approved booking -> customer pays -> payment succeeds -> booking confirmed -> receipt generated")
    void testScenario5_PaymentSucceedsAndReceiptGenerated() {
        User customer = getCustomerUser();
        // Use pre-seeded SLS-2026-00102 which is already APPROVED_AWAITING_PAYMENT
        Booking approvedBooking = bookingService.getBookingByReference("SLS-2026-00102");
        assertEquals(BookingStatus.APPROVED_AWAITING_PAYMENT, approvedBooking.getStatus());

        PaymentRequestDto payDto = new PaymentRequestDto();
        payDto.setBookingId(approvedBooking.getId());
        payDto.setCardHolderName("Johnathan Perera");
        payDto.setCardNumber("4532 1234 5678 9012");
        payDto.setExpiryDate("12/28");
        payDto.setCvv("456");
        payDto.setSimulateFailure(false);

        Payment payment = paymentService.processPayment(payDto, customer);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertNotNull(payment.getPaidAt());

        Booking confirmedBooking = bookingService.getBookingById(approvedBooking.getId());
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());

        // Receipt verification
        PaymentReceipt receipt = paymentService.getReceiptByPaymentId(payment.getId());
        assertNotNull(receipt);
        assertTrue(receipt.getReceiptNumber().startsWith("REC-SLS-"));
        assertTrue(receipt.getTotalPaid().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @Order(6)
    @DisplayName("Scenario 6: Payment fails -> customer can retry")
    void testScenario6_PaymentFailsAndAllowsRetry() {
        User customer = getCustomerUser();
        SafariSchedule schedule = scheduleRepository.findAll().get(0);
        SafariPackage pkg = packageRepository.findAll().get(0);

        // Create and approve a booking
        BookingRequestDto dto = new BookingRequestDto();
        dto.setScheduleId(schedule.getId());
        dto.setPackageId(pkg.getId());
        dto.setBookingDate(schedule.getScheduleDate());
        dto.setGuestAdults(1);
        Booking booking = bookingService.createBooking(dto, customer);
        bookingManagerService.approveBooking(booking.getId(), getBookingManagerUser(), "Approved for payment test.");

        // First attempt: simulate payment failure
        PaymentRequestDto failDto = new PaymentRequestDto();
        failDto.setBookingId(booking.getId());
        failDto.setCardHolderName("Johnathan Perera");
        failDto.setCardNumber("4532 0000 0000 0000"); // trigger decline
        failDto.setExpiryDate("05/27");
        failDto.setCvv("000");
        failDto.setSimulateFailure(true);

        assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processPayment(failDto, customer);
        });

        // Booking remains in APPROVED_AWAITING_PAYMENT state (allowing retry)
        Booking afterFail = bookingService.getBookingById(booking.getId());
        assertEquals(BookingStatus.APPROVED_AWAITING_PAYMENT, afterFail.getStatus());

        // Second attempt: retry with valid payment succeeds
        PaymentRequestDto retryDto = new PaymentRequestDto();
        retryDto.setBookingId(booking.getId());
        retryDto.setCardHolderName("Johnathan Perera");
        retryDto.setCardNumber("4532 8841 9012 3456");
        retryDto.setExpiryDate("12/28");
        retryDto.setCvv("789");
        retryDto.setSimulateFailure(false);

        Payment successfulPayment = paymentService.processPayment(retryDto, customer);
        assertEquals(PaymentStatus.PAID, successfulPayment.getStatus());
        assertEquals(BookingStatus.CONFIRMED, bookingService.getBookingById(booking.getId()).getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Scenario 7: Customer creates support ticket -> Manager responds -> ticket resolved")
    void testScenario7_SupportTicketWorkflow() {
        User customer = getCustomerUser();
        User manager = getBookingManagerUser();

        // 1. Customer creates ticket
        SupportTicketDto ticketDto = new SupportTicketDto();
        ticketDto.setSubject("Need clarification on Yala park entrance location");
        ticketDto.setDescription("Which gate will our safari jeep meet us at?");
        ticketDto.setCategory(com.safari.wildlife.enums.TicketCategory.INQUIRY);

        SupportTicket ticket = ticketService.createTicket(ticketDto, customer);
        assertEquals(TicketStatus.OPEN, ticket.getStatus());

        // 2. Manager responds and resolves
        TicketResponseDto responseDto = new TicketResponseDto(
                "Our driver meets all travelers at Palatupana Main Gate entrance at 05:45 AM.",
                TicketStatus.RESOLVED
        );
        TicketResponse response = ticketService.respondToTicket(ticket.getId(), responseDto, manager);

        assertNotNull(response);
        SupportTicket updatedTicket = ticketService.getTicketById(ticket.getId());
        assertEquals(TicketStatus.RESOLVED, updatedTicket.getStatus());
    }

    @Test
    @Order(8)
    @DisplayName("Scenario 8: Operations Manager creates destination and schedule")
    void testScenario8_OperationsManagerCreatesDestinationAndSchedule() {
        DestinationDto destDto = new DestinationDto();
        destDto.setName("Bundala National Park");
        destDto.setProvince("Southern Province");
        destDto.setDescription("UNESCO Biosphere Reserve renowned for migratory water birds and saline lagoons.");
        destDto.setOpeningTime("06:00 AM");
        destDto.setClosingTime("06:00 PM");

        Destination createdDest = destinationService.createDestination(destDto);
        assertNotNull(createdDest.getId());
        assertEquals("Bundala National Park", createdDest.getName());

        // Create Schedule for destination
        SafariPackage pkg = packageRepository.findAll().get(0);
        ScheduleDto schedDto = new ScheduleDto();
        schedDto.setDestinationId(createdDest.getId());
        schedDto.setPackageId(pkg.getId());
        schedDto.setScheduleDate(LocalDate.now().plusDays(10));
        schedDto.setTimeSlot("MORNING_0600");
        schedDto.setMaxSlots(18);

        SafariSchedule createdSched = scheduleService.createSchedule(schedDto);
        assertNotNull(createdSched.getId());
        assertEquals(createdDest.getId(), createdSched.getDestination().getId());
    }

    @Test
    @Order(9)
    @DisplayName("Scenario 9: Schedule conflict is detected")
    void testScenario9_ScheduleConflictDetected() {
        // Find existing schedule
        SafariSchedule existing = scheduleRepository.findAll().get(0);

        // Attempt to create identical destination, date, and slot
        ScheduleDto duplicateDto = new ScheduleDto();
        duplicateDto.setDestinationId(existing.getDestination().getId());
        duplicateDto.setPackageId(existing.getSafariPackage().getId());
        duplicateDto.setScheduleDate(existing.getScheduleDate());
        duplicateDto.setTimeSlot(existing.getTimeSlot());
        duplicateDto.setMaxSlots(20);

        assertThrows(ScheduleConflictException.class, () -> {
            scheduleService.createSchedule(duplicateDto);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Scenario 10: Fleet Manager assigns an available vehicle")
    void testScenario10_FleetManagerAssignsAvailableVehicle() {
        User fleetManager = getFleetManagerUser();
        // Booking SLS-2026-00103 is pre-seeded as CONFIRMED
        Booking confirmedBooking = bookingService.getBookingByReference("SLS-2026-00103");
        Vehicle availableVehicle = vehicleRepository.findByRegistrationNumberIgnoreCase("WP-SP-8821").orElseThrow();

        VehicleAssignment assignment = fleetService.assignVehicleToBooking(confirmedBooking.getId(), availableVehicle.getId(), fleetManager);
        assertNotNull(assignment);
        assertEquals(availableVehicle.getId(), assignment.getVehicle().getId());
    }

    @Test
    @Order(11)
    @DisplayName("Scenario 11: Vehicle under maintenance cannot be assigned")
    void testScenario11_VehicleUnderMaintenanceCannotBeAssigned() {
        User fleetManager = getFleetManagerUser();
        Booking confirmedBooking = bookingService.getBookingByReference("SLS-2026-00103");
        Vehicle maintVehicle = vehicleRepository.findByRegistrationNumberIgnoreCase("WP-SP-1092").orElseThrow();

        // Must throw VehicleUnavailableException
        assertThrows(VehicleUnavailableException.class, () -> {
            fleetService.assignVehicleToBooking(confirmedBooking.getId(), maintVehicle.getId(), fleetManager);
        });
    }

    @Test
    @Order(12)
    @DisplayName("Scenario 12: Finance Manager verifies payment and processes refund")
    void testScenario12_FinanceManagerProcessesRefund() {
        User financeManager = getFinanceManagerUser();
        // Payment for SLS-2026-00103 is pre-seeded and PAID
        Booking booking = bookingService.getBookingByReference("SLS-2026-00103");
        Payment payment = paymentService.getPaymentByBookingId(booking.getId());
        assertEquals(PaymentStatus.PAID, payment.getStatus());

        RefundRequestDto refundDto = new RefundRequestDto(
                payment.getId(),
                payment.getAmount(),
                "Customer flight cancellation refund."
        );

        Refund refund = paymentService.processRefund(refundDto, financeManager);
        assertNotNull(refund);
        assertEquals(payment.getAmount(), refund.getAmount());

        // Verify status transitions
        assertEquals(PaymentStatus.REFUNDED, paymentService.getPaymentById(payment.getId()).getStatus());
        assertEquals(BookingStatus.REFUNDED, bookingService.getBookingById(booking.getId()).getStatus());
    }

    @Test
    @Order(13)
    @DisplayName("Scenario 13: Admin creates user and assigns role")
    void testScenario13_AdminCreatesUserAndAssignsRole() {
        User admin = getAdminUser();

        UserManagementDto dto = new UserManagementDto();
        dto.setFullName("Kusal Mendis");
        dto.setEmail("kusal.mendis@safari.lk");
        dto.setPassword("Password123!");
        dto.setPhone("+94 77 555 1234");
        dto.setRole(UserRole.ROLE_OPERATIONS_MANAGER);
        dto.setActive(true);

        User createdUser = adminService.createUser(dto, admin);
        assertNotNull(createdUser.getId());
        assertEquals("kusal.mendis@safari.lk", createdUser.getEmail());
        assertEquals(UserRole.ROLE_OPERATIONS_MANAGER, createdUser.getRole().getName());
    }

    @Test
    @Order(14)
    @WithMockUser(username = "customer@safari.lk", roles = {"CUSTOMER"})
    @DisplayName("Scenario 14: Unauthorized user attempts to access restricted page -> access denied (403)")
    void testScenario14_UnauthorizedAccessBlockedWith403() throws Exception {
        // Customer attempting to access Admin API
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
}
