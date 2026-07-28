package com.safari.wildlife.config;

import com.safari.wildlife.entity.*;
import com.safari.wildlife.enums.*;
import com.safari.wildlife.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DestinationRepository destinationRepository;
    private final SafariPackageRepository packageRepository;
    private final PackageOptionRepository optionRepository;
    private final SafariScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final BookingCustomizationRepository customizationRepository;
    private final BookingApprovalRepository approvalRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final GuideRepository guideRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketResponseRepository responseRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CustomerRepository customerRepository,
                           DestinationRepository destinationRepository,
                           SafariPackageRepository packageRepository,
                           PackageOptionRepository optionRepository,
                           SafariScheduleRepository scheduleRepository,
                           BookingRepository bookingRepository,
                           BookingCustomizationRepository customizationRepository,
                           BookingApprovalRepository approvalRepository,
                           PaymentRepository paymentRepository,
                           PaymentReceiptRepository receiptRepository,
                           VehicleRepository vehicleRepository,
                           MaintenanceRecordRepository maintenanceRepository,
                           GuideRepository guideRepository,
                           SupportTicketRepository ticketRepository,
                           TicketResponseRepository responseRepository,
                           NotificationRepository notificationRepository,
                           AuditLogRepository auditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.destinationRepository = destinationRepository;
        this.packageRepository = packageRepository;
        this.optionRepository = optionRepository;
        this.scheduleRepository = scheduleRepository;
        this.bookingRepository = bookingRepository;
        this.customizationRepository = customizationRepository;
        this.approvalRepository = approvalRepository;
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.guideRepository = guideRepository;
        this.ticketRepository = ticketRepository;
        this.responseRepository = responseRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return; // Seed data already loaded
        }

        // 1. Roles
        Role customerRole = roleRepository.save(new Role(UserRole.ROLE_CUSTOMER, "Customer with booking, payment, and support capabilities"));
        Role bookingManagerRole = roleRepository.save(new Role(UserRole.ROLE_BOOKING_MANAGER, "Booking & Customer Relations Manager"));
        Role operationsRole = roleRepository.save(new Role(UserRole.ROLE_OPERATIONS_MANAGER, "Operations Manager for Destinations and Schedules"));
        Role fleetRole = roleRepository.save(new Role(UserRole.ROLE_FLEET_MANAGER, "Vehicle & Fleet Manager"));
        Role financeRole = roleRepository.save(new Role(UserRole.ROLE_FINANCE_MANAGER, "Finance Manager for Payments, Receipts, and Refunds"));
        Role adminRole = roleRepository.save(new Role(UserRole.ROLE_ADMIN, "System Administrator"));

        // 2. Demo Users (All have password: Password123!)
        String commonHash = passwordEncoder.encode("Password123!");

        User customerUser = userRepository.save(new User(customerRole, "Johnathan Perera", "customer@safari.lk", commonHash, "+94 77 123 4567"));
        Customer customerProfile = customerRepository.save(new Customer(customerUser, "Australian", "PA8821901", "72 Marine Drive, Colombo 03", "+61 412 345 678"));

        User bookingManager = userRepository.save(new User(bookingManagerRole, "Nimali Fernando", "booking@safari.lk", commonHash, "+94 71 234 5678"));
        User operationsManager = userRepository.save(new User(operationsRole, "Sunil Wickramasinghe", "operations@safari.lk", commonHash, "+94 72 345 6789"));
        User fleetManager = userRepository.save(new User(fleetRole, "Chaminda Silva", "fleet@safari.lk", commonHash, "+94 75 456 7890"));
        User financeManager = userRepository.save(new User(financeRole, "Kavinda Jayasuriya", "finance@safari.lk", commonHash, "+94 76 567 8901"));
        User adminUser = userRepository.save(new User(adminRole, "System Administrator", "admin@safari.lk", commonHash, "+94 70 678 9012"));

        // 3. Destinations
        Destination yala = destinationRepository.save(new Destination(
                "Yala National Park",
                "Southern & Uva Province",
                "World's highest concentration of leopards, sloth bears, Asian elephants, and magnificent coastal lagoons.",
                "Mandatory 4x4 safari jeep with registered guide. Do not alight from vehicle. Keep speed below 25 km/h.",
                "06:00 AM",
                "06:00 PM",
                "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?auto=format&fit=crop&w=1000&q=80"
        ));

        Destination wilpattu = destinationRepository.save(new Destination(
                "Wilpattu National Park",
                "North Western Province",
                "Sri Lanka's oldest and largest national park, characterized by over 100 natural sand-rimmed water basins (Willus).",
                "Strict noise control. Plastic bottles and litter strictly prohibited.",
                "06:00 AM",
                "06:00 PM",
                "https://images.unsplash.com/photo-1516426122078-c23e76319801?auto=format&fit=crop&w=1000&q=80"
        ));

        Destination minneriya = destinationRepository.save(new Destination(
                "Minneriya National Park",
                "North Central Province",
                "Famed for 'The Gathering' — an ancient dry season spectacle where up to 300 Asian elephants congregate on the reservoir banks.",
                "Maintain 20-meter distance from elephant herds. Flash photography prohibited.",
                "06:00 AM",
                "06:30 PM",
                "https://images.unsplash.com/photo-1557050543-4d5f4e07ef46?auto=format&fit=crop&w=1000&q=80"
        ));

        Destination udawalawe = destinationRepository.save(new Destination(
                "Udawalawe National Park",
                "Sabaragamuwa Province",
                "Vast grassland savannah surrounding the massive reservoir, renowned for constant year-round elephant sightings and birdlife.",
                "Respect wildlife right of way. Drones strictly prohibited without civil aviation permit.",
                "06:00 AM",
                "06:00 PM",
                "https://images.unsplash.com/photo-1534177616072-ef7dc120449d?auto=format&fit=crop&w=1000&q=80"
        ));

        // 4. Safari Packages
        SafariPackage yalaMorning = packageRepository.save(new SafariPackage(
                yala,
                "Yala Dawn Leopard Tracking Safari",
                SafariType.MORNING,
                new BigDecimal("120.00"),
                4,
                6,
                "Early morning game drive focusing on Block 1 big cats, Asian elephants, and morning bird activity.",
                "Park entry fees, tracker guide, chilled bottled water, park safari vehicle access"
        ));

        SafariPackage yalaAfternoon = packageRepository.save(new SafariPackage(
                yala,
                "Yala Golden Hour Sunset Safari",
                SafariType.AFTERNOON,
                new BigDecimal("135.00"),
                4,
                6,
                "Afternoon game drive catching wildlife near the coastal lagoons and rocky outcrops at sunset.",
                "Park entry fees, naturalist guide, binoculars, refreshments"
        ));

        SafariPackage minneriyaElephant = packageRepository.save(new SafariPackage(
                minneriya,
                "The Great Elephant Gathering Expedition",
                SafariType.AFTERNOON,
                new BigDecimal("110.00"),
                4,
                6,
                "Witness hundreds of wild elephants grazing and bathing along the scenic reservoir shores.",
                "Park permits, customized jeep, expert wildlife guide, water & fresh coconut juice"
        ));

        SafariPackage wilpattuFullDay = packageRepository.save(new SafariPackage(
                wilpattu,
                "Wilpattu Deep Wilderness All-Day Odyssey",
                SafariType.FULL_DAY,
                new BigDecimal("240.00"),
                10,
                6,
                "Full day deep exploration of Wilpattu's remote Willus tracking sloth bears, barking deer, and leopards.",
                "All-day park permit, packed bush lunch, professional naturalist, binoculars, iced beverages"
        ));

        // 5. Package Customization Options (Addons)
        PackageOption optJeepUpgrade = optionRepository.save(new PackageOption(
                yalaMorning,
                "Private 4x4 Open-Top Custom Jeep Upgrade",
                "Exclusive private modified Toyota Land Cruiser with 360-degree tiered viewing seats.",
                new BigDecimal("80.00")
        ));

        PackageOption optPhotoKit = optionRepository.save(new PackageOption(
                yalaMorning,
                "Wildlife Photography Kit & Telephoto Lens Rental",
                "Canon/Nikon DSLR equipped with 100-400mm telephoto zoom lens and bean-bag stabilizer.",
                new BigDecimal("45.00")
        ));

        PackageOption optBreakfast = optionRepository.save(new PackageOption(
                yalaMorning,
                "Gourmet Sri Lankan Bush Breakfast Hamper",
                "Fresh tropical fruit, warm egg rotti, coconut sambal, Ceylon tea, and fresh baked pastries.",
                new BigDecimal("25.00")
        ));

        PackageOption optBinoculars = optionRepository.save(new PackageOption(
                yalaMorning,
                "High-Power 10x42 Waterproof Binoculars Rental",
                "Nikon Monarch high-definition optical binoculars for crisp spotting.",
                new BigDecimal("15.00")
        ));

        optionRepository.save(new PackageOption(
                minneriyaElephant,
                "Private Custom Safari Jeep Upgrade",
                "Exclusive vehicle for your group only.",
                new BigDecimal("70.00")
        ));

        optionRepository.save(new PackageOption(
                minneriyaElephant,
                "Sunset High Tea on Minneriya Lake Ridge",
                "Ceylon tea and snacks with lake view.",
                new BigDecimal("30.00")
        ));

        // 6. Safari Schedules (Today + upcoming days)
        LocalDate today = LocalDate.now();
        SafariSchedule sched1 = scheduleRepository.save(new SafariSchedule(yala, yalaMorning, today.plusDays(1), "MORNING_0600", 20));
        SafariSchedule sched2 = scheduleRepository.save(new SafariSchedule(yala, yalaAfternoon, today.plusDays(1), "AFTERNOON_1430", 20));
        SafariSchedule sched3 = scheduleRepository.save(new SafariSchedule(minneriya, minneriyaElephant, today.plusDays(2), "AFTERNOON_1430", 25));
        SafariSchedule sched4 = scheduleRepository.save(new SafariSchedule(wilpattu, wilpattuFullDay, today.plusDays(3), "FULL_DAY_0600", 15));

        // 7. Vehicles
        Vehicle v1 = vehicleRepository.save(new Vehicle("WP-SP-8821", "Toyota Land Cruiser 4x4 Safari Custom", 6, 2022, true));
        Vehicle v2 = vehicleRepository.save(new Vehicle("WP-SP-9144", "Toyota Hilux Open-Top Safari Jeep", 6, 2021, false));
        Vehicle v3 = vehicleRepository.save(new Vehicle("SP-NC-3301", "Nissan Patrol Safari Spec 4x4", 7, 2023, true));
        
        // Under maintenance vehicle to verify Scenario 11
        Vehicle vMaint = new Vehicle("WP-SP-1092", "Toyota Land Cruiser Heavy-Duty 4x4", 6, 2019, true);
        vMaint.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        Vehicle savedMaintVehicle = vehicleRepository.save(vMaint);

        maintenanceRepository.save(new MaintenanceRecord(
                savedMaintVehicle,
                "Suspension & Brake Overhaul",
                today.minusDays(2),
                today.plusDays(5),
                new BigDecimal("420.00"),
                "Periodic heavy suspension bushings and brake rotor replacement following rough terrain safari."
        ));

        // 8. Safari Guides
        guideRepository.save(new Guide("Sampath Bandara", "LK-GL-1044", "English, Sinhala, German", "+94 77 987 6543", 12));
        guideRepository.save(new Guide("Roshan Alwis", "LK-GL-2198", "English, French, Sinhala", "+94 71 876 5432", 8));
        guideRepository.save(new Guide("Dinesh Karunaratne", "LK-GL-3910", "English, Italian", "+94 76 765 4321", 15));

        // 9. Pre-seeded Bookings across the workflow:
        // Booking A: PENDING (ready for Booking Manager to review/approve in Scenario 3 or reject in Scenario 4)
        Booking pendingBooking = new Booking(
                "SLS-2026-00101",
                customerProfile,
                sched1,
                yalaMorning,
                today.plusDays(1),
                2,
                0,
                new BigDecimal("320.00"), // 120*2 + 80 jeep upgrade
                "Please reserve the open-top jeep. We are keen on leopard photography."
        );
        pendingBooking.setStatus(BookingStatus.PENDING);
        Booking savedPending = bookingRepository.save(pendingBooking);
        customizationRepository.save(new BookingCustomization(savedPending, optJeepUpgrade, 1, optJeepUpgrade.getPrice()));
        sched1.addSlots(2);
        scheduleRepository.save(sched1);

        // Booking B: APPROVED_AWAITING_PAYMENT (ready for Customer to test payment in Scenario 5/6)
        Booking approvedBooking = new Booking(
                "SLS-2026-00102",
                customerProfile,
                sched2,
                yalaAfternoon,
                today.plusDays(1),
                2,
                1,
                new BigDecimal("352.50"), // (135*2 + 135*0.5) + 15 binocs
                "Traveling with 1 child. Need child safety booster."
        );
        approvedBooking.setStatus(BookingStatus.APPROVED_AWAITING_PAYMENT);
        Booking savedApproved = bookingRepository.save(approvedBooking);
        customizationRepository.save(new BookingCustomization(savedApproved, optBinoculars, 1, optBinoculars.getPrice()));
        approvalRepository.save(new BookingApproval(savedApproved, bookingManager, "APPROVED", "Validated customer details and schedule availability. Approved."));
        sched2.addSlots(3);
        scheduleRepository.save(sched2);

        // Booking C: CONFIRMED & PAID with Receipt (ready for Fleet dispatch in Scenario 10 or Finance refund in Scenario 12)
        Booking confirmedBooking = new Booking(
                "SLS-2026-00103",
                customerProfile,
                sched3,
                minneriyaElephant,
                today.plusDays(2),
                2,
                0,
                new BigDecimal("220.00"),
                "Anniversary trip celebration."
        );
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        Booking savedConfirmed = bookingRepository.save(confirmedBooking);
        sched3.addSlots(2);
        scheduleRepository.save(sched3);

        Payment payment = new Payment(savedConfirmed, "TXN-DEMO-991823", "CARD", new BigDecimal("220.00"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now().minusHours(3));
        Payment savedPayment = paymentRepository.save(payment);

        receiptRepository.save(new PaymentReceipt(
                savedPayment,
                "REC-SLS-44102",
                new BigDecimal("203.70"),
                new BigDecimal("16.30"),
                new BigDecimal("220.00")
        ));

        // 10. Pre-seeded Support Tickets
        SupportTicket openTicket = new SupportTicket(
                "TCK-8812",
                customerProfile,
                savedPending,
                TicketCategory.INQUIRY,
                "Inquiry on Hotel Pickup Location for Yala Safari",
                "Hello, does the dawn safari driver pick us up directly from Jetwing Yala or should we meet at the park gate?",
                "HIGH"
        );
        ticketRepository.save(openTicket);

        SupportTicket resolvedTicket = new SupportTicket(
                "TCK-7741",
                customerProfile,
                savedConfirmed,
                TicketCategory.BOOKING_CHANGE,
                "Vegetarian Bush Breakfast Request",
                "Could we please ensure the breakfast hamper includes strictly vegetarian options?",
                "MEDIUM"
        );
        resolvedTicket.setStatus(TicketStatus.RESOLVED);
        SupportTicket savedResolved = ticketRepository.save(resolvedTicket);
        responseRepository.save(new TicketResponse(savedResolved, bookingManager, "Confirmed! Our bush breakfast team will prepare a vegetarian gourmet hamper for your trip."));

        // 11. Initial Notifications
        notificationRepository.save(new Notification(customerUser, "Welcome to Sri Lanka Wild-Life Safaris!", "Explore our national park safaris and book your adventure.", "SYSTEM"));
        notificationRepository.save(new Notification(customerUser, "Booking Approved: SLS-2026-00102", "Your booking has been approved. You can now proceed to payment.", "BOOKING"));

        // 12. Initial Audit Logs
        auditLogRepository.save(new AuditLog(adminUser, "SYSTEM_INIT", "System", "0", "System initialized with base Sri Lankan wildlife safari data.", "127.0.0.1"));
    }
}
