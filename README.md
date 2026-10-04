# Web-Based Wild-Life Safari Trip Management System (Sri Lanka)

A fully functional, database-driven Web-Based Safari Trip Management System designed and built strictly according to Software Engineering project requirements.

---

## 1. Quick Start & Execution

The project is built with **Java 21** and **Spring Boot 3.3+**, with dual database capability (Zero-dependency portable MySQL-mode database for instant grading, and direct MySQL 8.0 support).

### Running the Application

In terminal / command prompt within the project directory:

```bash
mvn spring-boot:run
```

Or using the installed Maven binary directly:

```powershell
& "C:\Program Files\apache-maven-3.9.15\bin\mvn.cmd" spring-boot:run
```

Once started, open your browser and navigate to:
👉 **`http://localhost:8080`**

### Running Automated Test Suite (All 14 Scenarios)

```bash
mvn test
```

Or:

```powershell
& "C:\Program Files\apache-maven-3.9.15\bin\mvn.cmd" test
```

---

## 2. Default Test Accounts (Password for all: `Password123!`)

For instant evaluation, the top of every page features a **1-Click Demo Role Switcher**:

| User Role | Email | Password | Primary Permissions / Scope |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@safari.lk` | `Password123!` | Search, customize, book trips, pay, view receipts, submit support tickets, view notifications. |
| **Booking & Customer Relations Manager** | `booking@safari.lk` | `Password123!` | Review pending bookings, approve bookings, reject bookings with mandatory reason, respond to support tickets. |
| **Operations Manager** | `operations@safari.lk` | `Password123!` | Add/update/delete destinations, manage schedules, conflict detection. |
| **Vehicle & Fleet Manager** | `fleet@safari.lk` | `Password123!` | Register vehicles, log maintenance (auto-locks vehicles), manage guides, dispatch assignments. |
| **Finance Manager** | `finance@safari.lk` | `Password123!` | View payments ledger, inspect tax receipts, process refunds, view revenue analytics. |
| **System Administrator** | `admin@safari.lk` | `Password123!` | Manage user accounts, assign roles, activate/deactivate users, inspect system audit trail. |

---

## 3. Database Architecture & Schema

The relational database is normalized into 22 tables:

1. `roles` &ndash; RBAC user roles.
2. `users` &ndash; User credentials, BCrypt hashes, profile status.
3. `customers` &ndash; Customer personal details, nationality, passport/NIC.
4. `destinations` &ndash; Sri Lankan National Parks (Yala, Wilpattu, Minneriya, Udawalawe).
5. `safari_packages` &ndash; Morning, Afternoon, and Full Day safaris.
6. `package_options` &ndash; Customization addons (jeep upgrade, photo kit, bush breakfast).
7. `safari_schedules` &ndash; Dates, morning/afternoon time slots, and capacity limits.
8. `bookings` &ndash; Core booking entity with reference numbers (e.g. `SLS-2026-xxxxx`).
9. `booking_customizations` &ndash; Selected addons, quantities, and calculated prices.
10. `booking_approvals` &ndash; Booking manager approval/rejection audit records.
11. `payments` &ndash; Card transactions, transaction IDs, statuses (`PAID`, `FAILED`, `REFUNDED`).
12. `payment_receipts` &ndash; Official tax invoices with 8% VAT breakdown (`REC-SLS-xxxxx`).
13. `refunds` &ndash; Refund records and manager audit records (`REF-xxxxx`).
14. `vehicles` &ndash; Modified 4x4 safari jeeps, seating capacities, and status flags.
15. `maintenance_records` &ndash; Vehicle maintenance records that auto-lock vehicles.
16. `guides` &ndash; Certified multilingual wildlife trackers.
17. `vehicle_assignments` &ndash; Vehicle assignment to trips with slot conflict locks.
18. `guide_assignments` &ndash; Guide assignment to trips.
19. `support_tickets` &ndash; Customer inquiries and complaints (`TCK-xxxx`).
20. `ticket_responses` &ndash; Staff replies to customer tickets.
21. `notifications` &ndash; In-app notifications for workflow alerts.
22. `audit_logs` &ndash; Administrative audit log records.

---

## 4. Verification of the 14 Mandatory Project Scenarios

All 14 project scenarios are fully functional and verified in `SafariWorkflowTests.java`:

- **Scenario 1**: Customer selects package &rarr; customizes addons &rarr; price calculates live &rarr; creates booking (`PENDING`).
- **Scenario 2**: Invalid booking details (0 adults or missing dates) &rarr; system rejects and asks for correction.
- **Scenario 3**: Booking Manager reviews pending queue &rarr; approves booking &rarr; status transitions to `APPROVED_AWAITING_PAYMENT`.
- **Scenario 4**: Booking Manager rejects booking with reason &rarr; status transitions to `REJECTED`, schedule capacity released.
- **Scenario 5**: Approved booking &rarr; customer pays &rarr; status transitions to `CONFIRMED` &rarr; official receipt generated (`REC-SLS-xxxxx`).
- **Scenario 6**: Payment fails (simulated card decline) &rarr; customer receives failure notification and can retry.
- **Scenario 7**: Customer submits support ticket &rarr; manager responds &rarr; status updated to `RESOLVED`.
- **Scenario 8**: Operations Manager adds national park destination and creates new safari schedule.
- **Scenario 9**: Schedule conflict detected when duplicate destination, date, and slot is scheduled.
- **Scenario 10**: Fleet Manager assigns available vehicle (`WP-SP-8821`) to confirmed booking.
- **Scenario 11**: Vehicle under maintenance (`WP-SP-1092`) cannot be assigned &rarr; system throws `VehicleUnavailableException`.
- **Scenario 12**: Finance Manager verifies payment and processes customer refund &rarr; booking and payment updated to `REFUNDED`.
- **Scenario 13**: Admin creates user and assigns role &rarr; user is persisted with BCrypt encryption.
- **Scenario 14**: Unauthorized user attempts to access restricted endpoint (e.g. Customer accessing `/api/admin/users`) &rarr; HTTP 403 Forbidden is returned.
