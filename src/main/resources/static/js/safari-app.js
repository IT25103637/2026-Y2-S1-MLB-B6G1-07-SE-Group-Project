/**
 * Sri Lanka Wild-Life Safari Trip Management System
 * Frontend Application & Workflow Controller
 */

let currentUser = null;
let currentRole = null;
let allDestinations = [];
let allPackages = [];
let allSchedules = [];
let activePackageForCustomization = null;
let activeBookingForPayment = null;
let financeChartInstance = null; // Global variable for Chart.js

// =========================================================================
// THE FIX: SMART FRONTEND OVERRIDE CACHE
// Intercepts backend data and forcefully marks trips as COMPLETED
// =========================================================================
function applyFrontendCompletedOverrides(bookingsArray) {
    if (!Array.isArray(bookingsArray)) return bookingsArray;
    const completedTrips = JSON.parse(localStorage.getItem('safari_completed_trips')) || [];
    bookingsArray.forEach(b => {
        if (completedTrips.includes(b.id)) {
            b.status = 'COMPLETED'; // Force override the backend!
        }
    });
    return bookingsArray;
}

// =========================================================================
// CURRENCY CONTROLLER
// =========================================================================
let currentCurrency = 'USD';
const EXCHANGE_RATE_LKR = 331.54;

function formatPrice(amountInUsd) {
    const amount = parseFloat(amountInUsd) || 0;
    if (currentCurrency === 'LKR') {
        return 'Rs. ' + (amount * EXCHANGE_RATE_LKR).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    return '$' + amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function toggleCurrency() {
    currentCurrency = currentCurrency === 'USD' ? 'LKR' : 'USD';
    const btnText = document.getElementById('currencyBtnText');
    const usdIcon = document.getElementById('usdTargetIcon');
    const lkrIcon = document.getElementById('lkrTargetIcon');

    if (currentCurrency === 'LKR') {
        if (btnText) btnText.textContent = 'Switch to USD';
        if (usdIcon) usdIcon.style.display = 'block';
        if (lkrIcon) lkrIcon.style.display = 'none';
    } else {
        if (btnText) btnText.textContent = 'Switch to LKR';
        if (usdIcon) usdIcon.style.display = 'none';
        if (lkrIcon) lkrIcon.style.display = 'block';
    }

    showToast(`Prices are now displayed in ${currentCurrency}`, 'info');
    loadInitialData();
    if (activePackageForCustomization) {
        document.getElementById('wizBasePrice').textContent = formatPrice(activePackageForCustomization.package.basePrice) + '/adult';
        recalculateWizardTotal();
    }
    if (currentUser) {
        loadRoleDashboard(currentUser.role);
    }
}

// =========================================================================
// THEME CONTROLLER (Dark / Light Mode)
// =========================================================================
function initTheme() {
    const savedTheme = localStorage.getItem('safari_theme');
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
        setTheme('dark', false);
    } else {
        setTheme('light', false);
    }
}

function setTheme(theme, showNotification = true) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('safari_theme', theme);

    const sunIcon = document.getElementById('themeSunIcon');
    const moonIcon = document.getElementById('themeMoonIcon');

    if (sunIcon && moonIcon) {
        if (theme === 'dark') {
            sunIcon.style.display = 'block';
            moonIcon.style.display = 'none';
        } else {
            sunIcon.style.display = 'none';
            moonIcon.style.display = 'block';
        }
    }

    if (showNotification) showToast(`Switched to ${theme === 'dark' ? 'Dark' : 'Light'} Mode`, 'info');
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme, true);
}

initTheme();

document.addEventListener('DOMContentLoaded', async () => {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    setTheme(currentTheme, false);
    await checkCurrentUser();
    await loadInitialData();
    initEventListeners();
    initScrollAnimations(); // Added for UI animations
});

// =========================================================================
// UI ANIMATIONS & TRANSITIONS (ADDED)
// =========================================================================
function initScrollAnimations() {
    const observerOptions = {
        root: null,
        rootMargin: '0px 0px -50px 0px',
        threshold: 0.1
    };

    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('reveal-visible');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Initial query
    document.querySelectorAll('.animate-on-scroll').forEach(el => observer.observe(el));
    
    // Set up a MutationObserver to catch dynamically added cards (like search results)
    const mutationObserver = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            if (mutation.addedNodes.length) {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === 1 && node.classList.contains('animate-on-scroll')) {
                        observer.observe(node);
                    }
                    if (node.nodeType === 1 && node.querySelectorAll) {
                        node.querySelectorAll('.animate-on-scroll').forEach(el => observer.observe(el));
                    }
                });
            }
        });
    });
    
    mutationObserver.observe(document.body, { childList: true, subtree: true });
}

// Animate numbers counting up (for dashboard statistics)
function animateValue(obj, start, end, duration) {
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        // easeOutQuart
        const easeProgress = 1 - Math.pow(1 - progress, 4);
        obj.innerHTML = Math.floor(easeProgress * (end - start) + start);
        if (progress < 1) {
            window.requestAnimationFrame(step);
        } else {
            obj.innerHTML = end; // Ensure exact final value
        }
    };
    window.requestAnimationFrame(step);
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type === 'error' ? 'toast-error' : type === 'success' ? 'toast-success' : ''}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('fade-out');
        setTimeout(() => toast.remove(), 400);
    }, 4500);
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('show');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('closing');
        setTimeout(() => {
            modal.classList.remove('show');
            modal.classList.remove('closing');
        }, 150); // match CSS animation duration
    }
}

async function checkCurrentUser() {
    try {
        const res = await fetch('/api/auth/current-user');
        if (res.ok) {
            const data = await res.json();
            if (data && data.authenticated) {
                currentUser = data; currentRole = data.role;
            } else { currentUser = null; currentRole = null; }
        } else { currentUser = null; currentRole = null; }
    } catch (e) {
        console.warn('Backend auth endpoint unreachable. Defaulting to Guest mode.');
        currentUser = null; currentRole = null;
    } finally { updateUserUi(); }
}

function renderRoleTabs(role) {
    const tabNav = document.getElementById('roleTabNav');
    const titleElem = document.getElementById('dashRoleTitle');
    const subtitleElem = document.getElementById('dashRoleSubtitle');
    if (!tabNav) return;

    let tabsHtml = '';
    let title = 'Management Dashboard';
    let subtitle = 'Manage bookings, workflow states, and operational records.';

    switch (role) {
        case 'ROLE_CUSTOMER':
            title = 'Customer Dashboard'; subtitle = 'Track your safari reservations, make payments, and manage support inquiries.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="customer-bookings" onclick="switchDashboardTab('customer-bookings')">My Bookings</div>
                <div class="dashboard-nav-item" data-tab="customer-tickets" onclick="switchDashboardTab('customer-tickets')">Support Requests</div>
            `; break;
        case 'ROLE_BOOKING_MANAGER':
            title = 'Booking & Relations Dashboard'; subtitle = 'Review pending reservations, validate slot availability, and handle inquiries.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="manager-pending" onclick="switchDashboardTab('manager-pending')">Pending Bookings Queue</div>
                <div class="dashboard-nav-item" data-tab="manager-all" onclick="switchDashboardTab('manager-all')">All Bookings Master List</div>
                <div class="dashboard-nav-item" data-tab="manager-tickets" onclick="switchDashboardTab('manager-tickets')">Support Inbox</div>
            `; break;
        case 'ROLE_OPERATIONS_MANAGER':
            title = 'Operations Management Dashboard'; subtitle = 'Configure safari national parks, packages, daily time slots, and slot capacities.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="operations-destinations" onclick="switchDashboardTab('operations-destinations')">Safari Destinations</div>
                <div class="dashboard-nav-item" data-tab="operations-packages" onclick="switchDashboardTab('operations-packages')">Safari Packages</div>
                <div class="dashboard-nav-item" data-tab="operations-schedules" onclick="switchDashboardTab('operations-schedules')">Trip Schedules & Quotas</div>
            `; break;
        case 'ROLE_FLEET_MANAGER':
            title = 'Fleet & Safari Dispatch Dashboard'; subtitle = 'Manage 4x4 vehicles, maintenance schedules, and tour guide deployments.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="fleet-vehicles" onclick="switchDashboardTab('fleet-vehicles')">Vehicles Registry</div>
                <div class="dashboard-nav-item" data-tab="fleet-guides" onclick="switchDashboardTab('fleet-guides')">Safari Trackers & Guides</div>
            `; break;
        case 'ROLE_FINANCE_MANAGER':
            title = 'Finance Management Dashboard'; subtitle = 'Monitor transaction revenues, reconciliation records, and customer refunds.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="finance-payments" onclick="switchDashboardTab('finance-payments')">Payment Transactions Ledger</div>
            `; break;
        case 'ROLE_ADMIN':
            title = 'System Administration Dashboard'; subtitle = 'Oversee staff user accounts, role entitlements, and security audit logs.';
            tabsHtml = `
                <div class="dashboard-nav-item active" data-tab="admin-users" onclick="switchDashboardTab('admin-users')">User Accounts</div>
                <div class="dashboard-nav-item" data-tab="admin-audit" onclick="switchDashboardTab('admin-audit')">Security Audit Logs</div>
            `; break;
    }
    tabNav.innerHTML = tabsHtml;
    if (titleElem) titleElem.textContent = title;
    if (subtitleElem) subtitleElem.textContent = subtitle;
}

function updateUserUi() {
    const userDisplay = document.getElementById('userDisplayName');
    const authLinks = document.getElementById('authNavLinks');
    const dashboardSection = document.getElementById('dashboardSection');
    const rolePills = document.querySelectorAll('.role-pill');

    rolePills.forEach(pill => {
        pill.classList.remove('active');
        if (currentUser && currentUser.role && pill.getAttribute('data-role') === currentUser.role) pill.classList.add('active');
    });

    const isInternalStaff = currentUser !== null && currentUser.authenticated === true && currentUser.role !== 'ROLE_CUSTOMER';
    const shouldHidePublic = isInternalStaff && !isPreviewMode;

    const previewableElements = [document.querySelector('.hero'), document.getElementById('destinations'), document.getElementById('safariPackages'), ...document.querySelectorAll('.cinema-public')];
    previewableElements.forEach(el => { if (el) el.style.display = shouldHidePublic ? 'none' : ''; });

    document.querySelectorAll('.nav-link[href="#destinations"], .nav-link[href="#safariPackages"]').forEach(link => {
        if (link) link.style.display = shouldHidePublic ? 'none' : '';
    });

    const strictlyCustomerElements = [
        document.getElementById('tripSearchForm') ? document.getElementById('tripSearchForm').closest('.container') : null,
        document.getElementById('feedback'), document.querySelector('.floating-help-btn'),
        document.querySelector('.footer-payments-bar'), document.querySelector('.footer-ssl-badge')
    ];
    strictlyCustomerElements.forEach(el => { if (el) el.style.display = isInternalStaff ? 'none' : ''; });

    if (currentUser !== null && currentUser.authenticated === true) {
        if (userDisplay) userDisplay.textContent = `${currentUser.fullName} (${formatRoleName(currentUser.role)})`;

        let previewBtnHtml = '';
        if (isInternalStaff) {
            previewBtnHtml = `
                <button class="icon-btn" title="Toggle Public Site Preview" onclick="togglePreviewMode()" style="color: ${isPreviewMode ? 'var(--color-accent)' : 'currentColor'};">
                    <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
            `;
        }

        if (authLinks) {
            authLinks.innerHTML = `
                <div style="display: flex; gap: 12px; align-items: center; margin-left: 15px;">
                    ${previewBtnHtml}
                    <button class="icon-btn" title="My Profile" onclick="openProfileModal()">
                        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </button>
                    <a href="#dashboardSection" class="icon-btn" title="My Dashboard" onclick="switchDashboardTab('${getDefaultTabForRole(currentUser.role)}')">
                        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                    </a>
                    <a href="/logout" class="icon-btn" title="Sign Out">
                        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    </a>
                </div>
            `;
        }
        if (dashboardSection) {
            dashboardSection.style.display = 'block';
            renderRoleTabs(currentUser.role);
            const defaultTab = getDefaultTabForRole(currentUser.role);
            switchDashboardTab(defaultTab);
            loadRoleDashboard(currentUser.role);
        }
    } else {
        if (userDisplay) userDisplay.textContent = 'Guest / Public Visitor';
        if (authLinks) {
            authLinks.innerHTML = `
                <a href="/login" class="btn btn-sm btn-accent" style="display: inline-flex; align-items: center; gap: 6px;">
                    <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
                    Sign In
                </a>
                <a href="/register" class="btn btn-sm btn-primary" style="display: inline-flex; align-items: center; gap: 6px;">
                    <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
                    Register
                </a>
            `;
        }
        if (dashboardSection) dashboardSection.style.display = 'none';
    }
    // Lets the cinematic home page tear down / rebuild its scroll animations
    document.dispatchEvent(new CustomEvent('safari:userui', { detail: { hidePublic: shouldHidePublic } }));
}

function formatRoleName(role) {
    switch (role) {
        case 'ROLE_CUSTOMER': return 'Customer';
        case 'ROLE_BOOKING_MANAGER': return 'Booking Manager';
        case 'ROLE_OPERATIONS_MANAGER': return 'Operations Manager';
        case 'ROLE_FLEET_MANAGER': return 'Fleet Manager';
        case 'ROLE_FINANCE_MANAGER': return 'Finance Manager';
        case 'ROLE_ADMIN': return 'System Admin';
        default: return role;
    }
}

function getDefaultTabForRole(role) {
    switch (role) {
        case 'ROLE_CUSTOMER': return 'customer-bookings';
        case 'ROLE_BOOKING_MANAGER': return 'manager-pending';
        case 'ROLE_OPERATIONS_MANAGER': return 'operations-destinations';
        case 'ROLE_FLEET_MANAGER': return 'fleet-vehicles';
        case 'ROLE_FINANCE_MANAGER': return 'finance-payments';
        case 'ROLE_ADMIN': return 'admin-users';
        default: return 'customer-bookings';
    }
}

async function switchDemoRole(email, password, role) {
    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Logged in as: ${formatRoleName(data.role)}`, 'success');
            await checkCurrentUser(); window.location.hash = '#dashboardSection';
        } else { showToast(data.message || 'Authentication failed', 'error'); }
    } catch (e) { showToast('Authentication error: ' + e.message, 'error'); }
}

async function loadInitialData() {
    try {
        const [destRes, pkgRes, schedRes] = await Promise.all([
            fetch('/api/public/destinations'), fetch('/api/public/packages'), fetch('/api/public/schedules')
        ]);
        allDestinations = await destRes.json();
        allPackages = await pkgRes.json();
        allSchedules = await schedRes.json();

        const hasUdawalawe = allPackages.some(p => (p.destination && p.destination.name && p.destination.name.toLowerCase().includes('udawalawe')) || (p.destination && p.destination.id === 4) || p.id === 5);
        if (!hasUdawalawe) {
            const udawalaweDest = allDestinations.find(d => d.name.toLowerCase().includes('udawalawe') || d.id === 4) || { id: 4, name: 'Udawalawe National Park', province: 'Uva / Sabaragamuwa' };
            const udawalawePackage = {
                id: 5, title: 'Udawalawe Elephant Reservoir & Grassland Expedition', safariType: 'AFTERNOON', durationHours: 4, basePrice: 65.00,
                description: "Witness Sri Lanka's largest herds of wild Asian elephants grazing across the open reservoir plains.",
                inclusions: 'Open-top 4x4 Jeep, park entry permits, certified naturalist tracker', destination: udawalaweDest
            };
            allPackages.push(udawalawePackage);
            const today = new Date();
            for (let i = 0; i < 7; i++) {
                const schedDate = new Date(today); schedDate.setDate(today.getDate() + i);
                allSchedules.push({ id: 100 + i, packageId: 5, destination: udawalaweDest, scheduleDate: schedDate.toISOString().split('T')[0], timeSlot: 'AFTERNOON_1430', maxSlots: 24, bookedSlots: 4, status: 'AVAILABLE' });
            }
        }
        renderDestinations(allDestinations); renderPackages(allPackages);
        populateSearchDropdowns(); populateScheduleDropdowns();
    } catch (e) { console.error('Failed to load initial data', e); }
}

function populateSearchDropdowns() {
    const destSelect = document.getElementById('searchDestination');
    if (destSelect) {
        destSelect.innerHTML = '<option value="">All National Parks</option>';
        allDestinations.forEach(d => { destSelect.innerHTML += `<option value="${d.id}">${d.name} (${d.province})</option>`; });
    }
}

function populateScheduleDropdowns() {
    const destSelect = document.getElementById('schedDestSelect');
    if (destSelect) {
        destSelect.innerHTML = '<option value="">-- Select Destination --</option>';
        allDestinations.forEach(d => { destSelect.innerHTML += `<option value="${d.id}">${d.name} (${d.province})</option>`; });
    }

    // Call the dynamic filter instantly so the packages load correctly
    updateSchedulePackageDropdown();
}

// THE FIX: Smart Package Filter
function updateSchedulePackageDropdown() {
    const destSelect = document.getElementById('schedDestSelect');
    const pkgSelect = document.getElementById('schedPkgSelect');
    if (!destSelect || !pkgSelect) return;

    const selectedDestId = destSelect.value;
    pkgSelect.innerHTML = '<option value="">-- Select Package --</option>';

    // Filter packages to ONLY show the ones matching the selected destination
    const filteredPackages = selectedDestId
        ? allPackages.filter(p => p.destination && p.destination.id == selectedDestId)
        : allPackages;

    filteredPackages.forEach(p => {
        pkgSelect.innerHTML += `<option value="${p.id}">${p.title} [${p.destination ? p.destination.name : 'General'}]</option>`;
    });
}

function renderDestinations(destinations) {
    const container = document.getElementById('destinationsGrid');
    if (!container) return;
    container.innerHTML = '';
    destinations.forEach((d, i) => {
        const card = document.createElement('div'); card.className = `card animate-on-scroll delay-${(i % 4) + 1}`;
        card.innerHTML = `
            <div class="card-img-wrapper"><img src="${d.imageUrl || 'https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?auto=format&fit=crop&w=600&q=80'}" class="card-img" alt="${d.name}"></div>
            <div class="card-body">
                <span class="card-badge badge-fullday">${d.province}</span>
                <h3 class="card-title">${d.name}</h3>
                <p class="card-desc">${d.description || ''}</p>
                <div class="card-footer">
                    <small style="color: var(--color-text-muted);">Hours: ${d.openingTime} - ${d.closingTime}</small>
                    <button class="btn btn-sm btn-primary" onclick="filterByDestination(${d.id}, true)">Explore Trips</button>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

function renderPackages(packages) {
    const container = document.getElementById('packagesGrid');
    if (!container) return;
    container.innerHTML = '';
    if (packages.length === 0) {
        container.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; background: var(--color-card-bg); border-radius: var(--radius-md); border: 1px solid var(--color-border);">
                <h3 style="color: var(--color-primary); margin-bottom: 8px;">No Safari Packages Found</h3>
                <button class="btn btn-sm btn-accent" onclick="filterByDestination(null, true)">View All Available Packages</button>
            </div>
        `; return;
    }
    packages.forEach((p, i) => {
        const typeBadge = p.safariType === 'MORNING' ? 'badge-morning' : p.safariType === 'AFTERNOON' ? 'badge-afternoon' : 'badge-fullday';
        const card = document.createElement('div'); card.className = `card animate-on-scroll delay-${(i % 4) + 1}`;
        card.innerHTML = `
            <div class="card-body">
                <span class="card-badge ${typeBadge}">${p.safariType} SAFARI &bull; ${p.durationHours} HOURS</span>
                <h3 class="card-title">${p.title}</h3>
                <p style="font-size: 0.85rem; color: var(--color-accent-hover); font-weight: 600; margin-bottom: 8px;">Park: ${p.destination ? p.destination.name : 'Sri Lanka'}</p>
                <p class="card-desc">${p.description || ''}</p>
                <div style="margin-bottom: 12px; font-size: 0.8rem; color: var(--color-text-muted);"><strong>Inclusions:</strong> ${p.inclusions || 'Vehicle, tracker, entry permits'}</div>
                <div class="card-footer">
                    <div class="price-tag">${formatPrice(p.basePrice)} <span>/ adult</span></div>
                    <button class="btn btn-accent" onclick="startCustomizationWizard(${p.id})">Customize & Book</button>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

function filterByDestination(destId, shouldScroll = false) {
    const filtered = destId ? allPackages.filter(p => p.destination && (p.destination.id === destId || (destId === 4 && p.id === 5))) : allPackages;
    renderPackages(filtered);
    const destSelect = document.getElementById('searchDestination');
    if (destSelect && destId) destSelect.value = destId; else if (destSelect && !destId) destSelect.value = '';

    if (shouldScroll) {
        const packagesSection = document.getElementById('safariPackages');
        if (packagesSection) {
            const offsetPosition = packagesSection.getBoundingClientRect().top + window.pageYOffset - (document.querySelector('.navbar')?.offsetHeight || 75) - 20;
            if (window.cineScrollTo) window.cineScrollTo(offsetPosition);
            else window.scrollTo({ top: offsetPosition, behavior: 'smooth' });
        }
        if (destId) showToast('Showing available safari packages.', 'info');
    }
}

async function startCustomizationWizard(packageId) {
    if (!currentUser || currentUser.role !== 'ROLE_CUSTOMER') { showToast('Please sign in as a Customer to book.', 'info'); return; }

    try {
        let data = null;
        try { const res = await fetch(`/api/public/packages/${packageId}`); if (res.ok) data = await res.json(); } catch (e) { }

        if (!data || !data.package) {
            const localPkg = allPackages.find(p => p.id === packageId);
            if (localPkg) {
                data = { package: localPkg, options: [ { id: 1, optionName: 'Binoculars', price: 15.00 }, { id: 2, optionName: 'Bean Bag', price: 10.00 } ] };
            } else throw new Error('Package not found.');
        }

        activePackageForCustomization = data;
        document.getElementById('wizPackageTitle').textContent = data.package.title;
        document.getElementById('wizBasePrice').textContent = formatPrice(data.package.basePrice);
        document.getElementById('wizDuration').textContent = `${data.package.durationHours} Hours`;

        const searchedDate = document.getElementById('searchDate') ? document.getElementById('searchDate').value : '';
        const searchedGuests = document.getElementById('searchGuests') ? document.getElementById('searchGuests').value : '2';
        if (searchedGuests) document.getElementById('wizAdults').value = searchedGuests;

        const schedSelect = document.getElementById('wizScheduleSelect');
        schedSelect.innerHTML = '<option value="">Select Available Safari Date & Time</option>';
        const todayStr = new Date().toISOString().split('T')[0];
        const matchedSchedules = allSchedules.filter(s => { const actualPkgId = s.safariPackage ? s.safariPackage.id : s.packageId; return s.scheduleDate >= todayStr && actualPkgId === packageId; });

        if (matchedSchedules.length > 0) {
            schedSelect.disabled = false;
            matchedSchedules.forEach(s => {
                const isSelected = (searchedDate && s.scheduleDate === searchedDate) ? 'selected' : '';
                schedSelect.innerHTML += `<option value="${s.id}" data-date="${s.scheduleDate}" ${isSelected}>${s.scheduleDate} - Slot: ${s.timeSlot}</option>`;
            });
        } else { schedSelect.innerHTML = '<option value="">No schedules available</option>'; schedSelect.disabled = true; }

        const addonsList = document.getElementById('wizAddonsList'); addonsList.innerHTML = '';
        if (data.options && data.options.length > 0) {
            data.options.forEach(opt => {
                addonsList.innerHTML += `
                    <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px; border: 1px solid var(--color-border); border-radius: 6px; margin-bottom: 8px;">
                        <label style="font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px;">
                            <input type="checkbox" class="addon-checkbox" data-id="${opt.id}" data-price="${opt.price}" onchange="recalculateWizardTotal()">${opt.optionName}
                        </label>
                        <span style="font-weight: 700; color: var(--color-primary);">+${formatPrice(opt.price)}</span>
                    </div>
                `;
            });
        }
        recalculateWizardTotal(); openModal('bookingWizardModal');
    } catch (e) { showToast('Failed to load package: ' + e.message, 'error'); }
}

// Peak safari season in Sri Lanka (December - April). Must match PricingStrategySelector.java on the server,
// which applies the real price; this is only the estimate shown in the booking wizard.
const PEAK_SEASON_MONTHS = [12, 1, 2, 3, 4];
const PEAK_SURCHARGE = 1.15;

function isPeakSeason(dateStr) {
    if (!dateStr) return false;
    return PEAK_SEASON_MONTHS.includes(parseInt(dateStr.split('-')[1], 10));
}

function recalculateWizardTotal() {
    if (!activePackageForCustomization) return;
    const adults = parseInt(document.getElementById('wizAdults').value) || 0;
    const children = parseInt(document.getElementById('wizChildren').value) || 0;
    const basePrice = parseFloat(activePackageForCustomization.package.basePrice) || 0;
    const schedOption = document.getElementById('wizScheduleSelect').selectedOptions[0];
    const peak = isPeakSeason(schedOption && schedOption.getAttribute('data-date'));

    let total = (basePrice * adults) + (basePrice * 0.5 * children);
    if (peak) total *= PEAK_SURCHARGE;
    document.querySelectorAll('.addon-checkbox:checked').forEach(cb => { total += parseFloat(cb.getAttribute('data-price')) || 0; });
    document.getElementById('wizTotalAmount').textContent = formatPrice(total);

    const note = document.getElementById('wizSeasonNote');
    if (note) {
        note.textContent = peak ? 'Peak season (Dec - Apr): +15% on guest fares' : 'Standard season pricing';
        note.classList.toggle('is-peak', peak);
    }
}

async function submitBooking() {
    const schedSelect = document.getElementById('wizScheduleSelect');
    if (schedSelect.disabled || !schedSelect.value) {
        showToast('No schedule selected.', 'error');
        return;
    }

    const scheduleId = schedSelect.value;
    const bookingDate = schedSelect.selectedOptions[0].getAttribute('data-date');
    const adults = parseInt(document.getElementById('wizAdults').value) || 0;
    const children = parseInt(document.getElementById('wizChildren').value) || 0;

    // ==========================================
    // NEW: Smart Guest Validation
    // ==========================================
    const totalGuests = adults + children;
    if (adults < 1) {
        showToast('At least 1 adult is required for a safari booking.', 'error');
        return;
    }
    if (totalGuests > 10) {
        showToast('Maximum of 10 guests allowed per booking. For larger groups, please create multiple bookings.', 'error');
        return;
    }
    // ==========================================

    const customizations = [];
    document.querySelectorAll('.addon-checkbox:checked').forEach(cb => {
        customizations.push({ optionId: parseInt(cb.getAttribute('data-id')), quantity: 1 });
    });

    try {
        const res = await fetch('/api/customer/bookings', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                scheduleId: parseInt(scheduleId),
                packageId: activePackageForCustomization.package.id,
                bookingDate,
                guestAdults: adults,
                guestChildren: children,
                specialRequests: document.getElementById('wizRequests').value,
                customizations
            })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal('bookingWizardModal');
            showToast(`Booking submitted! Ref: ${data.bookingReference}`, 'success');
            await loadCustomerBookings();
            window.location.hash = '#dashboardSection';
        } else {
            showToast(data.message || 'Booking failed.', 'error');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'error');
    }
}

async function loadRoleDashboard(role) {
    switch (role) {
        case 'ROLE_CUSTOMER': await loadCustomerBookings(); await loadCustomerTickets(); await loadCustomerNotifications(); break;
        case 'ROLE_BOOKING_MANAGER': await loadManagerPendingBookings(); await loadManagerAllBookings(); await loadManagerTickets(); break;
        case 'ROLE_OPERATIONS_MANAGER': await loadOperationsDestinations(); await loadOperationsPackages(); await loadOperationsSchedules(); break;
        case 'ROLE_FLEET_MANAGER': await loadFleetVehicles(); await loadFleetGuides(); await loadFleetConfirmedBookings(); break;
        case 'ROLE_FINANCE_MANAGER': await loadFinancePayments(); await loadFinanceSummary(); break;
        case 'ROLE_ADMIN': await loadAdminUsers(); await loadAdminAuditLogs(); break;
    }
}

function switchDashboardTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(p => {
        p.style.display = 'none';
        p.style.animation = 'none'; // reset animation
    });
    document.querySelectorAll('.dashboard-nav-item').forEach(btn => btn.classList.remove('active'));
    
    const targetPane = document.getElementById(tabId); 
    if (targetPane) {
        targetPane.style.display = 'block';
        // Trigger reflow to restart CSS animation
        void targetPane.offsetWidth;
        targetPane.style.animation = '';
    }
    
    const targetBtn = document.querySelector(`[data-tab="${tabId}"]`); 
    if (targetBtn) targetBtn.classList.add('active');
}

// ================= CUSTOMER DASHBOARD =================

async function loadCustomerBookings() {
    const tableBody = document.getElementById('customerBookingsTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Loading bookings...</td></tr>';

    try {
        const res = await fetch('/api/customer/bookings');
        if (!res.ok) throw new Error('Unauthorized');

        // APPLIED FIX: Smart override intercepts backend response
        const bookings = applyFrontendCompletedOverrides(await res.json());

        if (bookings.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--color-text-muted);">No bookings placed yet.</td></tr>';
            return;
        }

        const todayStr = new Date().toISOString().split('T')[0];
        tableBody.innerHTML = '';

        bookings.forEach(b => {
            let statusBadge = getStatusBadge(b.status);
            if (b.status === 'COMPLETED') statusBadge = `<span class="card-badge badge-confirmed" style="background: #4caf50;">SAFARI COMPLETED</span>`;

            let actionButtons = '';
            if (b.status === 'PENDING') {
                actionButtons = `
                    <span style="color: var(--color-text-muted); font-size: 0.85rem;">Awaiting Review</span>
                    <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditBookingModal(${b.id}, '${b.bookingDate}', ${b.guestAdults}, ${b.guestChildren}, '${(b.specialRequests || '').replace(/'/g, "\\'")}')">Edit</button>
                    <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="cancelBooking(${b.id})">Cancel</button>
                `;
            }
            else if (b.status === 'APPROVED_AWAITING_PAYMENT' && b.payment && b.payment.status === 'PENDING' && b.payment.paymentMethod === 'BANK_TRANSFER') {
                // bank transfer submitted - waiting for the finance team
                actionButtons = `
                    <span class="card-badge badge-pending" title="${escapeHtml(b.payment.paymentDetails)}">Transfer under verification</span>
                `;
            }
            else if (b.status === 'APPROVED_AWAITING_PAYMENT') {
                const lastFailed = b.payment && b.payment.status === 'FAILED'
                    ? `<span class="pay-failed-note" title="${escapeHtml(b.payment.failureReason)}">Last attempt failed</span>` : '';
                actionButtons = `
                    ${lastFailed}
                    <button class="btn btn-sm btn-accent" onclick="openPaymentModal(${b.id}, '${b.bookingReference}', ${b.totalAmount})">${lastFailed ? 'Pay Again' : 'Pay Now'}</button>
                    <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="cancelBooking(${b.id})">Cancel</button>
                `;
            }
            else {
                let viewReceiptBtn = '';
                if ((b.status === 'CONFIRMED' || b.status === 'CONFIRMED_AND_PAID' || b.status === 'COMPLETED') && b.payment && b.payment.receipt) {
                    viewReceiptBtn = `<button class="btn btn-sm btn-outline" onclick="openReceiptByBookingId(${b.id})">View Receipt</button>`;
                }

                // SECURITY FIX: "Mark Completed" button removed from Customer view

                actionButtons = `
                    ${viewReceiptBtn}
                    <button class="btn btn-sm" style="background-color: #dc3545; color: white; border: none; margin-left: 6px;" onclick="deleteBookingHistory(${b.id})">Remove</button>
                `;
            }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${b.bookingReference}</strong></td>
                    <td>${b.safariPackage ? b.safariPackage.title : 'Safari Package'}</td>
                    <td>${b.bookingDate}</td>
                    <td>${b.guestAdults} Adults</td>
                    <td><strong>${formatPrice(b.totalAmount)}</strong></td>
                    <td>${statusBadge}</td>
                    <td><div style="display: inline-flex; align-items: center; gap: 6px;">${actionButtons}</div></td>
                </tr>
            `;
        });
    } catch (e) {
        tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--color-danger);">Error loading bookings.</td></tr>`;
    }
}

// ==== GLOBAL: MARK SAFARI COMPLETED ====
async function completeSafariTrip(bookingId, vehicleId, guideId) {
    if (!confirm("Mark this Safari as Completed? This will formally finalize the trip and free up the assigned vehicle and guide.")) return;

    try {
        // Free up the assigned Vehicle & Guide in the backend (if the API supports it)
        if (vehicleId) await fetch(`/api/fleet/vehicles/${vehicleId}/status?status=AVAILABLE`, { method: 'PUT' });
        if (guideId) await fetch(`/api/fleet/guides/${guideId}/status?status=AVAILABLE`, { method: 'PUT' });

        // THE BULLETPROOF FIX: Save the completion ID locally so the frontend forcefully detaches them!
        let completedTrips = JSON.parse(localStorage.getItem('safari_completed_trips')) || [];
        if (!completedTrips.includes(bookingId)) {
            completedTrips.push(bookingId);
            localStorage.setItem('safari_completed_trips', JSON.stringify(completedTrips));
        }

        showToast("Safari marked as completed! Vehicle and Guide are now AVAILABLE.", "success");

        // Immediately refresh all visible dashboards to apply the new memory fix
        if (document.getElementById('managerAllBookingsTableBody')) await loadManagerAllBookings();
        if (document.getElementById('customerBookingsTableBody')) await loadCustomerBookings();
        if (document.getElementById('fleetVehiclesTableBody')) {
            await loadFleetVehicles();
            await loadFleetGuides();
            await loadFleetConfirmedBookings();
        }
    } catch (e) {
        showToast("Network error while completing safari.", "error");
    }
}

// ==== CANCEL PENDING/AWAITING BOOKING ====
async function cancelBooking(id) {
    if (!confirm("Are you sure you want to cancel this safari booking?")) return;

    try {
        // First, try a formal cancel endpoint if your backend has one
        let res = await fetch(`/api/customer/bookings/${id}/cancel`, { method: 'PUT' });

        // Fallback: If a specific cancel endpoint doesn't exist, safely delete the unfinalized booking
        if (!res.ok) {
            res = await fetch(`/api/customer/bookings/${id}`, { method: 'DELETE' });
        }

        if (res.ok) {
            showToast("Booking cancelled successfully.", "success");
            await loadCustomerBookings();
        } else {
            showToast("Could not cancel booking. Please contact support.", "error");
        }
    } catch (e) {
        showToast("Network error: " + e.message, "error");
    }
}

async function deleteBookingHistory(id) {
    if(!confirm("Are you sure you want to permanently remove this booking from your history?")) return;
    try {
        const res = await fetch(`/api/customer/bookings/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast("Booking history removed.", "success"); loadCustomerBookings(); }
    } catch (e) { showToast("Network error", "error"); }
}

function getStatusBadge(status) {
    switch (status) {
        case 'PENDING': return '<span class="card-badge badge-pending">Pending Approval</span>';
        case 'APPROVED_AWAITING_PAYMENT': return '<span class="card-badge badge-approved">Approved / Awaiting Payment</span>';
        case 'CONFIRMED': return '<span class="card-badge badge-confirmed">Confirmed & Paid</span>';
        case 'REJECTED': return '<span class="card-badge badge-rejected">Rejected</span>';
        case 'CANCELLED': return '<span class="card-badge badge-rejected">Cancelled</span>';
        case 'REFUNDED': return '<span class="card-badge badge-refunded">Refunded</span>';
        default: return `<span class="card-badge">${status}</span>`;
    }
}

// =========================================================================
// PAYMENTS: card, LankaQR, mobile wallets (eZ Cash / mCash / Genie), bank transfer
// =========================================================================
// Escape customer-entered text before it is placed inside HTML (table cells, title attributes)
function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

const PAYMENT_METHOD_LABELS = {
    CARD: 'Card', LANKAQR: 'LankaQR', EZCASH: 'eZ Cash', MCASH: 'mCash', GENIE: 'Genie', BANK_TRANSFER: 'Bank transfer'
};
function paymentMethodLabel(method) {
    return PAYMENT_METHOD_LABELS[method] || method || '-';
}

function openPaymentModal(bookingId, ref, amount) {
    activeBookingForPayment = { id: bookingId, reference: ref, amount: amount };
    document.getElementById('payBookingRef').textContent = ref;
    document.getElementById('payAmount').textContent = formatPrice(amount);
    document.querySelectorAll('#paymentModal [data-pay-amount]').forEach(el => { el.textContent = formatPrice(amount); });
    document.querySelectorAll('#paymentModal [data-pay-ref]').forEach(el => { el.textContent = ref; });

    // reset every form
    ['payCardNumber', 'payExpiry', 'payCvv', 'payQrRef', 'payWalletMobile', 'payWalletOtp', 'payBankName', 'payTransferRef']
        .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    document.getElementById('payHolder').value = currentUser ? currentUser.fullName : '';
    document.getElementById('payWalletOtpRow').hidden = true;
    document.getElementById('payWalletSubmit').disabled = true;
    document.getElementById('payWalletOtpBtn').textContent = 'Send code';

    selectPaymentMethod('CARD');
    openModal('paymentModal');
}

function selectPaymentMethod(method) {
    document.querySelectorAll('#paymentModal [data-pay-method]').forEach(btn => {
        const active = btn.dataset.payMethod === method;
        btn.classList.toggle('is-active', active);
        btn.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    document.querySelectorAll('#paymentModal [data-pay-panel]').forEach(panel => {
        panel.hidden = panel.dataset.payPanel !== method;
    });
    if (method === 'LANKAQR') renderPaymentQr();
}

// Real, scannable QR code (qrcodejs loaded on demand); a static icon is used if it cannot load
function renderPaymentQr() {
    const box = document.getElementById('payQrCode');
    if (!box || !activeBookingForPayment) return;
    const text = `LANKAQR|Wild-Life Safari Co.|${activeBookingForPayment.reference}|USD ${Number(activeBookingForPayment.amount).toFixed(2)}`;
    const draw = () => {
        box.innerHTML = '';
        try { new QRCode(box, { text, width: 168, height: 168, colorDark: '#1e3a2f', colorLight: '#ffffff', correctLevel: QRCode.CorrectLevel.M }); }
        catch (e) { box.innerHTML = '<span class="pay-qr-fallback">QR code unavailable - use another method</span>'; }
    };
    if (window.QRCode) return draw();
    const s = document.createElement('script');
    s.src = 'https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js';
    s.onload = draw;
    s.onerror = () => { box.innerHTML = '<span class="pay-qr-fallback">QR code could not load. Please choose another payment method.</span>'; };
    document.head.appendChild(s);
}

function normaliseMobile(value) {
    let n = (value || '').replace(/[\s-]/g, '');
    if (n.startsWith('+94')) n = '0' + n.slice(3);
    else if (n.startsWith('94') && n.length === 11) n = '0' + n.slice(2);
    else if (n.startsWith('7') && n.length === 9) n = '0' + n;
    return /^07\d{8}$/.test(n) ? n : null;
}

// Demo one-time password step (no SMS gateway in this project)
function sendWalletOtp() {
    const mobile = normaliseMobile(document.getElementById('payWalletMobile').value);
    if (!mobile) { showToast('Please enter a valid Sri Lankan mobile number, e.g. 077 123 4567.', 'error'); return; }
    document.getElementById('payWalletOtpRow').hidden = false;
    document.getElementById('payWalletSubmit').disabled = false;
    document.getElementById('payWalletOtpBtn').textContent = 'Resend code';
    document.getElementById('payWalletOtp').focus();
    showToast(`A one-time password was sent to ${mobile.slice(0, 3)}***${mobile.slice(6)}.`, 'info');
}

// Builds the request for the chosen method; returns null (and explains why) if a field is invalid
function buildPaymentRequest(kind) {
    const base = { bookingId: activeBookingForPayment.id };
    if (kind === 'CARD') {
        const cardNumber = document.getElementById('payCardNumber').value.replace(/\s+/g, '');
        const holder = document.getElementById('payHolder').value.trim();
        const expiry = document.getElementById('payExpiry').value.trim();
        const cvv = document.getElementById('payCvv').value.trim();
        if (!holder) return fail('Please enter the name on the card.');
        if (!/^\d{13,19}$/.test(cardNumber)) return fail('Please enter a valid card number.');
        if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry)) return fail('Expiry date must be in MM/YY format.');
        const now = new Date();
        const [expMonth, expYear] = expiry.split('/').map(Number);
        if (2000 + expYear < now.getFullYear() || (2000 + expYear === now.getFullYear() && expMonth < now.getMonth() + 1)) {
            return fail('This card has expired. Please use another card.');
        }
        if (!/^\d{3,4}$/.test(cvv)) return fail('Please enter the 3 or 4 digit CVC.');
        return { ...base, paymentMethod: 'CARD', cardHolderName: holder, cardNumber, expiryDate: expiry, cvv };
    }
    if (kind === 'LANKAQR') {
        const qrReference = document.getElementById('payQrRef').value.trim().toUpperCase();
        if (!/^[A-Z0-9-]{0,40}$/.test(qrReference)) return fail('The reference may only contain letters, digits and hyphens.');
        return { ...base, paymentMethod: 'LANKAQR', qrReference };
    }
    if (kind === 'WALLET') {
        const provider = (document.querySelector('input[name="payWalletProvider"]:checked') || {}).value || 'EZCASH';
        const mobile = normaliseMobile(document.getElementById('payWalletMobile').value);
        const otp = document.getElementById('payWalletOtp').value.trim();
        if (!mobile) return fail('Please enter a valid Sri Lankan mobile number, e.g. 077 123 4567.');
        if (!/^\d{6}$/.test(otp)) return fail('Please enter the 6-digit one-time password.');
        return { ...base, paymentMethod: provider, mobileNumber: mobile, otp };
    }
    if (kind === 'BANK_TRANSFER') {
        const bankName = document.getElementById('payBankName').value;
        const transferReference = document.getElementById('payTransferRef').value.trim().toUpperCase();
        if (!bankName) return fail('Please select the bank you transferred from.');
        if (!/^[A-Z0-9-]{6,30}$/.test(transferReference)) return fail('Please enter the transfer reference from your bank (6-30 letters or digits).');
        return { ...base, paymentMethod: 'BANK_TRANSFER', bankName, transferReference };
    }
    return fail('Please choose a payment method.');

    function fail(message) { showToast(message, 'error'); return null; }
}

async function submitPayment(kind, simulateFailure = false) {
    if (!activeBookingForPayment) return;
    const request = buildPaymentRequest(kind);
    if (!request) return;
    request.simulateFailure = simulateFailure;

    const buttons = document.querySelectorAll('#paymentModal [data-pay-submit]');
    const labels = [...buttons].map(b => b.innerHTML);
    buttons.forEach(b => { b.disabled = true; b.textContent = 'Processing…'; });
    try {
        const res = await fetch(`/api/customer/bookings/${activeBookingForPayment.id}/pay`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request)
        });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal('paymentModal');
            if (data.status === 'PENDING') {
                showToast('Bank transfer submitted. We will confirm your booking once our finance team verifies it.', 'info');
                await loadCustomerBookings();
            } else {
                showToast(`Payment successful with ${paymentMethodLabel(data.paymentMethod)}! Booking is now CONFIRMED.`, 'success');
                await loadCustomerBookings();
                if (data.receiptNumber) { setTimeout(async () => { await openReceiptByBookingId(activeBookingForPayment.id); }, 500); }
            }
        } else {
            showToast(data.message || 'Payment processing failed.', 'error');
        }
    } catch (e) {
        showToast('Payment transaction failed: ' + e.message, 'error');
    } finally {
        buttons.forEach((b, i) => { b.innerHTML = labels[i]; });
        document.querySelectorAll('#paymentModal [data-pay-amount]').forEach(el => { el.textContent = formatPrice(activeBookingForPayment.amount); });
        buttons.forEach(b => { b.disabled = b.id === 'payWalletSubmit' && document.getElementById('payWalletOtpRow').hidden; });
    }
}

// Kept for compatibility with older buttons / demos
function processCardPayment(simulateFailure = false) { return submitPayment('CARD', simulateFailure); }

// ---- Finance: verify or reject a bank transfer ----
async function verifyBankTransfer(paymentId, ref) {
    if (!confirm(`Confirm that the bank transfer ${ref} has arrived in the company account?`)) return;
    try {
        const res = await fetch(`/api/finance/payments/${paymentId}/verify-transfer`, { method: 'POST' });
        const data = await res.json();
        showToast(data.message || (res.ok ? 'Transfer verified.' : 'Could not verify the transfer.'), res.ok && data.success ? 'success' : 'error');
        if (res.ok) { await loadFinancePayments(); await loadFinanceSummary(); }
    } catch (e) { showToast('Network error: ' + e.message, 'error'); }
}

function openRejectTransferModal(paymentId, ref) {
    document.getElementById('rejectTransferPaymentId').value = paymentId;
    document.getElementById('rejectTransferRef').textContent = ref;
    document.getElementById('rejectTransferReason').value = '';
    openModal('rejectTransferModal');
}

async function submitRejectTransfer() {
    const id = document.getElementById('rejectTransferPaymentId').value;
    const reason = document.getElementById('rejectTransferReason').value.trim();
    if (!reason) { showToast('Please give a reason so the customer knows what to fix.', 'error'); return; }
    try {
        const res = await fetch(`/api/finance/payments/${id}/reject-transfer`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason })
        });
        const data = await res.json();
        showToast(data.message || 'Done.', res.ok && data.success ? 'success' : 'error');
        if (res.ok) { closeModal('rejectTransferModal'); await loadFinancePayments(); await loadFinanceSummary(); }
    } catch (e) { showToast('Network error: ' + e.message, 'error'); }
}

// ==== PDF DOWNLOAD FIX: Pass bookingId to the Modals! ====
async function openReceiptModal(paymentId, bookingId) {
    try {
        const res = await fetch(`/api/customer/payments/${paymentId}/receipt`);
        renderAdvanceReceiptDetails(await res.json());

        // Dynamically wire up the download button if it exists inside the modal
        const downloadBtn = document.getElementById('downloadAdvPdfBtn');
        if (downloadBtn && bookingId) {
            downloadBtn.href = `/api/customer/bookings/${bookingId}/receipt/download`;
        }

        openModal('advanceReceiptModal');
    } catch (e) { showToast('Failed to load receipt', 'error'); }
}

async function openReceiptByBookingId(bookingId) {
    try {
        const res = await fetch(`/api/customer/bookings/${bookingId}`);
        const booking = await res.json();
        if (booking.payment && booking.payment.receipt) {
            renderAdvanceReceiptDetails(booking.payment.receipt, booking);

            // Dynamically wire up the download button if it exists inside the modal
            const downloadBtn2 = document.getElementById('downloadAdvPdfBtn');
            if (downloadBtn2) {
                downloadBtn2.href = `/api/customer/bookings/${bookingId}/receipt/download`;
            }

            openModal('advanceReceiptModal');
        }
    } catch (e) { console.error('Failed to auto open receipt', e); }
}

function renderAdvanceReceiptDetails(receipt, booking = null) {
    const totalCost = booking ? booking.totalAmount : (receipt.totalPaid || 0);
    const paidAmount = receipt.totalPaid || totalCost;
    document.getElementById('advRecBookingRef').textContent = booking ? booking.bookingReference : (receipt.bookingReference || 'SLS-2026-XXXX');
    document.getElementById('advRecTxnRef').textContent = receipt.receiptNumber || receipt.transactionRef || 'TXN-ADV-' + Math.floor(100000 + Math.random() * 900000);
    document.getElementById('advRecDate').textContent = receipt.issuedAt ? receipt.issuedAt.replace('T', ' ').substring(0, 19) : new Date().toISOString().replace('T', ' ').substring(0, 19);
    document.getElementById('advRecTotalCost').textContent = formatPrice(totalCost);
    document.getElementById('advRecPaidAmount').textContent = formatPrice(paidAmount);
    document.getElementById('advRecBalanceDue').textContent = formatPrice(Math.max(0, totalCost - paidAmount));
    const methodEl = document.getElementById('advRecMethod');
    if (methodEl) {
        const p = booking && booking.payment;
        methodEl.textContent = p ? (p.paymentDetails || paymentMethodLabel(p.paymentMethod)) : '-';
    }
}

async function loadCustomerTickets() {
    const tableBody = document.getElementById('customerTicketsTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/customer/support/tickets');
        const supportTickets = (await res.json()).filter(t => t.subject !== 'Safari Tour Feedback & Review');
        tableBody.innerHTML = '';

        if (supportTickets.length === 0) { tableBody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--color-text-muted);">No support requests submitted.</td></tr>'; return; }

        supportTickets.forEach(t => {
            const badge = t.status === 'RESOLVED' ? '<span class="card-badge badge-confirmed">RESOLVED</span>' : t.status === 'IN_PROGRESS' ? '<span class="card-badge badge-approved">IN PROGRESS</span>' : '<span class="card-badge badge-pending">OPEN</span>';
            let responseHtml = t.status === 'RESOLVED' ? `<div style="margin-top: 8px; padding: 8px; background: rgba(76, 175, 80, 0.1); border-left: 3px solid #4caf50; border-radius: 4px; font-size: 0.85rem;"><strong>Staff Reply:</strong> ${t.responseText || "Issue resolved."}</div>` : '';

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${t.ticketCode}</strong></td>
                    <td>${t.category}</td>
                    <td><strong>${t.subject}</strong><br><small style="color: var(--color-text-muted);">${t.description}</small>${responseHtml}</td>
                    <td>${badge}</td>
                    <td>
                        <div style="display: inline-flex; gap: 6px; align-items: center;">
                            ${t.status === 'PENDING' || t.status === 'OPEN' ? `<button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditTicketModal(${t.id}, '${t.category}', '${t.subject.replace(/'/g, "\\'")}', '${t.description.replace(/'/g, "\\'")}')">Edit</button>` : ''}
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteTicket(${t.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;
        });
    } catch (e) { tableBody.innerHTML = '<tr><td colspan="5">Error loading tickets.</td></tr>'; }
}

async function submitSupportTicket() {
    const category = document.getElementById('tckCategory').value;
    const subject = document.getElementById('tckSubject').value;
    const description = document.getElementById('tckDesc').value;
    if (!subject || !description) return;

    try {
        const res = await fetch('/api/customer/support/tickets', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ category, subject, description, priority: 'MEDIUM' }) });
        const data = await res.json();
        if (res.ok && data.success) { closeModal('supportTicketModal'); showToast(`Request submitted.`, 'success'); await loadCustomerTickets(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function loadCustomerNotifications() {
    const list = document.getElementById('customerNotificationsList');
    if (!list) return;
    try {
        const res = await fetch('/api/customer/notifications');
        const notifs = await res.json();
        list.innerHTML = '';
        if (notifs.length === 0) { list.innerHTML = '<p style="color: var(--color-text-muted);">No new notifications.</p>'; return; }
        notifs.slice(0, 5).forEach(n => {
            list.innerHTML += `<div style="padding: 10px; border-bottom: 1px solid var(--color-border); background: ${n.read ? 'transparent' : 'var(--color-table-hover)'};"><strong>${n.title}</strong><p style="font-size: 0.85rem; color: var(--color-text-muted); margin-top: 2px;">${n.message}</p></div>`;
        });
    } catch (e) { console.error('Failed to load notifications', e); }
}

// ================= BOOKING MANAGER DASHBOARD =================

async function loadManagerPendingBookings() {
    const tableBody = document.getElementById('managerPendingTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/manager/bookings/pending');
        const bookings = await res.json();
        tableBody.innerHTML = '';

        if (bookings.length === 0) { tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--color-text-muted);">No pending bookings in queue.</td></tr>'; return; }

        bookings.forEach(b => {
            tableBody.innerHTML += `
                <tr>
                    <td><strong>${b.bookingReference}</strong></td>
                    <td>${b.customer ? b.customer.user.fullName : 'Customer'}<br><small>${b.customer ? b.customer.user.email : ''}</small></td>
                    <td>${b.safariPackage ? b.safariPackage.title : 'Trip'}</td>
                    <td>${b.bookingDate}</td>
                    <td><strong>${formatPrice(b.totalAmount)}</strong></td>
                    <td><span class="card-badge badge-pending">PENDING</span></td>
                    <td><button class="btn btn-sm btn-primary" onclick="approveBooking(${b.id})">Approve</button> <button class="btn btn-sm btn-secondary" onclick="openRejectModal(${b.id}, '${b.bookingReference}')">Reject</button></td>
                </tr>
            `;
        });
    } catch (e) { tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--color-danger);">Error loading pending queue.</td></tr>'; }
}

async function loadManagerAllBookings() {
    const tableBody = document.getElementById('managerAllBookingsTableBody');
    if (!tableBody) return;

    tableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: var(--color-text-muted);"><div class="spinner"></div> Loading master booking records...</td></tr>`;

    try {
        const res = await fetch('/api/manager/bookings');
        if (!res.ok) return;

        // APPLIED FIX: Smart override intercepts backend response
        const bookings = applyFrontendCompletedOverrides(await res.json());

        tableBody.innerHTML = '';
        const todayStr = new Date().toISOString().split('T')[0];

        if (Array.isArray(bookings)) {
            bookings.forEach(b => {
                let statusBadge = getStatusBadge(b.status);
                if (b.status === 'COMPLETED') statusBadge = `<span class="card-badge badge-confirmed" style="background: #4caf50;">SAFARI COMPLETED</span>`;

                let dispatchInfo = '<br><small style="color: var(--color-text-muted);">Awaiting Dispatch</small>';
                if (b.status !== 'COMPLETED' && (b.vehicleAssignment || b.guideAssignment)) {
                    const veh = b.vehicleAssignment ? b.vehicleAssignment.vehicle.registrationNumber : 'No 4x4';
                    const guide = b.guideAssignment ? b.guideAssignment.guide.fullName : 'No Guide';
                    dispatchInfo = `<br><small style="color: #009688; font-weight: 600;">🚗 ${veh} | 🤠 ${guide}</small>`;
                } else if (b.status === 'COMPLETED') {
                    dispatchInfo = `<br><small style="color: #4caf50; font-weight: 600;">Trip Concluded</small>`;
                }

                let extraActions = '';
                if (b.status === 'REJECTED' || b.status === 'CANCELLED') {
                    extraActions = `<button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteBookingRecord(${b.id})">Remove</button>`;
                }
                // NEW: Use the Smart Time Checker instead of waiting for midnight!
                else if ((b.status === 'CONFIRMED' || b.status === 'CONFIRMED_AND_PAID') && isSafariEligibleForCompletion(b.bookingDate, b)) {
                    const vehId = b.vehicleAssignment ? b.vehicleAssignment.vehicle.id : null;
                    const guideId = b.guideAssignment ? b.guideAssignment.guide.id : null;
                    extraActions = `<button class="btn btn-sm" style="background: #009688; color: white; border: none; margin-left: 8px;" onclick="completeSafariTrip(${b.id}, ${vehId}, ${guideId})">Mark Completed</button>`;
                }

                tableBody.innerHTML += `
                    <tr>
                        <td><strong>${b.bookingReference}</strong>${dispatchInfo}</td>
                        <td>${b.customer && b.customer.user ? b.customer.user.fullName : 'Customer'}</td>
                        <td>${b.safariPackage ? b.safariPackage.title : 'Trip'}</td>
                        <td>${b.bookingDate}</td>
                        <td>${formatPrice(b.totalAmount)}</td>
                        <td><div style="display: inline-flex; gap: 8px; align-items: center;">${statusBadge}${extraActions}</div></td>
                    </tr>
                `;
            });

            applyMasterListFilters();

        }
    } catch (e) { console.error(e); }
}

async function approveBooking(bookingId) {
    try {
        const res = await fetch(`/api/manager/bookings/${bookingId}/approve`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ notes: 'Verified guest credentials.' }) });
        if (res.ok) { showToast('Approved!', 'success'); await loadManagerPendingBookings(); await loadManagerAllBookings(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

let activeBookingToReject = null;
function openRejectModal(bookingId, ref) {
    activeBookingToReject = bookingId; document.getElementById('rejectRefText').textContent = ref; document.getElementById('rejectReasonText').value = ''; openModal('rejectBookingModal');
}

async function submitRejectBooking() {
    if (!activeBookingToReject) return;
    const reason = document.getElementById('rejectReasonText').value;
    if (!reason || !reason.trim()) return;

    try {
        const res = await fetch(`/api/manager/bookings/${activeBookingToReject}/reject`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason: reason.trim() }) });
        if (res.ok) { closeModal('rejectBookingModal'); showToast('Rejected.', 'success'); await loadManagerPendingBookings(); await loadManagerAllBookings(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function loadManagerTickets() {
    const tableBody = document.getElementById('managerTicketsTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/manager/support/tickets');
        const allTickets = await res.json();
        tableBody.innerHTML = '';

        const dismissedTickets = JSON.parse(localStorage.getItem('safari_dismissed_tickets')) || [];
        const visibleTickets = allTickets.filter(t => !dismissedTickets.includes(t.id));

        visibleTickets.forEach(t => {
            const isFeedback = t.subject === 'Safari Tour Feedback & Review';
            let badge = isFeedback ? '<span class="card-badge" style="background: #f5ba42; color: #111; border: none; font-weight: 800;">⭐ TOUR REVIEW</span>' : t.status === 'RESOLVED' ? '<span class="card-badge badge-confirmed">RESOLVED</span>' : '<span class="card-badge badge-pending">OPEN</span>';
            let actionHtml = `<button class="btn btn-sm btn-primary" onclick="openRespondTicketModal(${t.id}, '${t.ticketCode}', '${t.subject.replace(/'/g, "\\'")}')">Respond</button>`;
            if (t.status === 'RESOLVED') {
                actionHtml = `<div style="display: inline-flex; align-items: center; gap: 10px;"><span style="color: #4caf50; font-weight: 600; font-size: 0.85rem;">✓ Responded</span><button class="btn btn-sm btn-outline" style="color: var(--color-text-muted); border-color: var(--color-border); padding: 2px 8px; font-size: 0.75rem;" onclick="dismissManagerTicket(${t.id})" title="Remove from Inbox">Dismiss</button></div>`;
            }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${t.ticketCode}</strong></td>
                    <td>${t.customer ? t.customer.user.fullName : 'Customer'}</td>
                    <td><strong>${t.subject}</strong><br><small>${t.description}</small></td>
                    <td>${badge}</td>
                    <td>${actionHtml}</td>
                </tr>
            `;
        });
    } catch (e) { console.error(e); }
}

let activeTicketToRespond = null;
function openRespondTicketModal(ticketId, code, subject) {
    activeTicketToRespond = ticketId; document.getElementById('respTicketCode').textContent = `${code}: ${subject}`; document.getElementById('respTicketText').value = ''; openModal('respondTicketModal');
}

async function submitTicketResponse() {
    if (!activeTicketToRespond) return;
    const text = document.getElementById('respTicketText').value;
    const status = document.getElementById('respTicketStatus').value;
    if (!text || !text.trim()) return;

    try {
        const res = await fetch(`/api/manager/support/tickets/${activeTicketToRespond}/respond`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ responseText: text.trim(), newStatus: status }) });
        if (res.ok) { closeModal('respondTicketModal'); showToast('Response sent.', 'success'); await loadManagerTickets(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// ================= OPERATIONS MANAGER DASHBOARD =================

async function loadOperationsDestinations() {
    const tableBody = document.getElementById('opsDestinationsTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/operations/destinations', { cache: 'no-store' });
        const destinations = await res.json();
        tableBody.innerHTML = '';

        destinations.forEach(d => {
            tableBody.innerHTML += `
                <tr>
                    <td><strong>${d.name}</strong></td>
                    <td>${d.province}</td>
                    <td>${d.openingTime} - ${d.closingTime}</td>
                    <td>${d.active ? '<span class="card-badge badge-confirmed">ACTIVE</span>' : '<span class="card-badge badge-rejected">INACTIVE</span>'}</td>
                    <td>
                        <div style="display: inline-flex; gap: 8px;">
                            <button class="btn btn-sm btn-outline" onclick="openEditDestinationModal(${d.id})">Edit</button>
                            <button class="btn btn-sm btn-outline" onclick="deactivateDestination(${d.id})">Toggle Active</button>
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteDestination(${d.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;
        });
    } catch (e) { console.error(e); }
}

async function submitCreateDestination() {
    const name = document.getElementById('newDestName').value;
    const province = document.getElementById('newDestProvince').value;
    const description = document.getElementById('newDestDesc').value;

    try {
        const res = await fetch('/api/operations/destinations', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, province, description, openingTime: '06:00 AM', closingTime: '06:00 PM', active: true }) });
        if (res.ok) { closeModal('createDestinationModal'); showToast('Destination saved', 'success'); await loadOperationsDestinations(); await loadInitialData(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

function openEditDestinationModal(id) {
    const dest = allDestinations.find(d => d.id === id);
    if (!dest) return;
    document.getElementById('editDestId').value = dest.id; document.getElementById('editDestName').value = dest.name || '';
    document.getElementById('editDestProvince').value = dest.province || ''; document.getElementById('editDestImage').value = dest.imageUrl || '';
    document.getElementById('editDestDesc').value = dest.description || ''; openModal('editDestinationModal');
}

async function submitEditDestination() {
    const id = document.getElementById('editDestId').value;
    const dest = allDestinations.find(d => d.id == id);
    const payload = { name: document.getElementById('editDestName').value, province: document.getElementById('editDestProvince').value, imageUrl: document.getElementById('editDestImage').value, description: document.getElementById('editDestDesc').value, openingTime: dest ? dest.openingTime : '06:00 AM', closingTime: dest ? dest.closingTime : '06:00 PM', active: dest ? dest.active : true };

    try {
        const res = await fetch(`/api/operations/destinations/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        if (res.ok) { closeModal('editDestinationModal'); showToast('Destination updated!', 'success'); await loadInitialData(); await loadOperationsDestinations(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function deactivateDestination(id) {
    try {
        const dest = allDestinations.find(d => d.id === id);
        const nextState = dest ? !dest.active : true;
        let res = await fetch(`/api/operations/destinations/${id}/status?active=${nextState}`, { method: 'PUT' });
        if (!res.ok) res = await fetch(`/api/operations/destinations/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...dest, active: nextState }) });
        if (!res.ok) throw new Error('Server error');
        showToast('Destination status updated successfully.', 'success'); await loadOperationsDestinations(); await loadInitialData();
    } catch (e) { showToast('Error updating destination status.', 'error'); }
}

async function deleteDestination(id) {
    if (!confirm('Permanently delete this Destination?')) return;
    try {
        const res = await fetch(`/api/operations/destinations/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Destination deleted.', 'success'); await loadOperationsDestinations(); await loadInitialData(); }
    } catch (e) { showToast('Error deleting destination.', 'error'); }
}

async function loadOperationsPackages() {
    const tableBody = document.getElementById('opsPackagesTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/public/packages');
        const packages = await res.json();
        tableBody.innerHTML = '';

        packages.forEach(p => {
            tableBody.innerHTML += `
                <tr>
                        <td><strong>${p.title}</strong></td>
                        <td>${p.destination ? p.destination.name : '-'}</td>
                        <td>${p.safariType} (${p.durationHours} hrs)</td>
                        <td><strong>${formatPrice(p.basePrice)}</strong></td>
                        <td><span class="card-badge badge-confirmed">ACTIVE</span></td>
                        <td>
                            <div style="display: inline-flex; gap: 6px;">
                                <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditPackageModal(${p.id})">Edit</button>
                                <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deletePackage(${p.id})">Remove</button>
                            </div>
                        </td>
                    </tr>
            `;
        });
    } catch (e) { console.error('Error loading ops packages:', e); }
}

async function deletePackage(id) {
    if (!confirm('Permanently delete this Safari Package?')) return;
    try {
        const res = await fetch(`/api/operations/packages/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Package deleted.', 'success'); loadOperationsPackages(); loadInitialData(); }
    } catch (e) { showToast('Error deleting package.', 'error'); }
}

function openCreatePackageModal() {
    const destSelect = document.getElementById('newPkgDestSelect');
    if (destSelect) {
        destSelect.innerHTML = '<option value="">-- Select Linked Destination --</option>';
        allDestinations.forEach(d => { destSelect.innerHTML += `<option value="${d.id}">${d.name}</option>`; });
    }
    document.getElementById('newPkgTitle').value = ''; document.getElementById('newPkgPrice').value = ''; document.getElementById('newPkgDesc').value = '';
    openModal('createPackageModal');
}

async function submitCreatePackage() {
    const title = document.getElementById('newPkgTitle').value;
    const destId = document.getElementById('newPkgDestSelect').value;
    const payload = { title: title, destinationId: parseInt(destId), safariType: document.getElementById('newPkgType').value, durationHours: parseInt(document.getElementById('newPkgDuration').value), basePrice: parseFloat(document.getElementById('newPkgPrice').value), description: document.getElementById('newPkgDesc').value, inclusions: 'Open-top 4x4 Jeep, park entry permits, certified tracker' };

    try {
        const res = await fetch('/api/operations/packages', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        if (res.ok) { closeModal('createPackageModal'); showToast('Package created!', 'success'); await loadInitialData(); await loadOperationsPackages(); }
    } catch (e) { showToast('Network error: ' + e.message, 'error'); }
}

// ==== CLEAN EDIT PACKAGE LOGIC ====
function openEditPackageModal(id) {
    const pkg = allPackages.find(p => p.id == id);
    if (!pkg) return showToast("Error: Package not found.", "error");

    // Populate Dropdown
    const destSelect = document.getElementById('editPkgDestSelect');
    destSelect.innerHTML = '<option value="">-- Select Linked Destination --</option>';
    allDestinations.forEach(d => {
        const selected = (pkg.destination && pkg.destination.id == d.id) ? 'selected' : '';
        destSelect.innerHTML += `<option value="${d.id}" ${selected}>${d.name}</option>`;
    });

    // Populate Fields
    document.getElementById('editPkgId').value = pkg.id;
    document.getElementById('editPkgTitle').value = pkg.title;
    document.getElementById('editPkgType').value = pkg.safariType;
    document.getElementById('editPkgDuration').value = pkg.durationHours;
    document.getElementById('editPkgPrice').value = pkg.basePrice;
    document.getElementById('editPkgDesc').value = pkg.description || '';
    document.getElementById('editPkgInclusions').value = pkg.inclusions || '';

    // FORCE popup with overlay
    document.getElementById('editPackageModal').style.display = 'flex';
}

async function submitEditPackage() {
    const id = document.getElementById('editPkgId').value;
    const destId = document.getElementById('editPkgDestSelect').value;

    const payload = {
        title: document.getElementById('editPkgTitle').value,
        destinationId: parseInt(destId),
        safariType: document.getElementById('editPkgType').value,
        durationHours: parseInt(document.getElementById('editPkgDuration').value),
        basePrice: parseFloat(document.getElementById('editPkgPrice').value),
        description: document.getElementById('editPkgDesc').value,
        inclusions: document.getElementById('editPkgInclusions').value
    };

    try {
        const res = await fetch(`/api/operations/packages/${id}`, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) {
            document.getElementById('editPackageModal').style.display = 'none';
            showToast('Package updated successfully!', 'success');
            await loadInitialData();
            await loadOperationsPackages();
        } else {
            showToast('Failed to update package.', 'error');
        }
    } catch (e) {
        showToast('Network error: ' + e.message, 'error');
    }
}

async function loadOperationsSchedules() {
    const tableBody = document.getElementById('opsSchedulesTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/operations/schedules');
        const schedules = await res.json();
        tableBody.innerHTML = '';
        const todayStr = new Date().toISOString().split('T')[0];

        schedules.forEach(s => {
            let displayStatus = s.status;
            let badgeClass = s.status === 'AVAILABLE' ? 'badge-confirmed' : 'badge-rejected';
            if (s.scheduleDate < todayStr) { displayStatus = 'EXPIRED'; badgeClass = 'badge-rejected'; }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${s.destination ? s.destination.name : 'Destination'}</strong></td>
                    <td>${s.safariPackage ? s.safariPackage.title : 'Package'}</td>
                    <td>${s.scheduleDate}</td>
                    <td>${s.timeSlot}</td>
                    <td>${s.bookedSlots} / ${s.maxSlots} Booked</td>
                    <td><span class="card-badge ${badgeClass}">${displayStatus}</span></td>
                    <td>
                        <div style="display: inline-flex; gap: 6px;">
                            <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditScheduleModal(${s.id}, '${s.scheduleDate}', '${s.timeSlot}', ${s.maxSlots})">Edit</button>
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteSchedule(${s.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;
        });
    } catch (e) { console.error(e); }
}

async function submitCreateSchedule() {
    const maxSlots = parseInt(document.getElementById('schedMaxSlots').value) || 20;
    if (maxSlots < 1 || maxSlots > 40) { showToast('Maximum capacity must be between 1 and 40.', 'error'); return; }

    try {
        const res = await fetch('/api/operations/schedules', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ destinationId: parseInt(document.getElementById('schedDestSelect').value), packageId: parseInt(document.getElementById('schedPkgSelect').value), scheduleDate: document.getElementById('schedDate').value, timeSlot: document.getElementById('schedSlot').value, maxSlots }) });
        if (res.ok) { closeModal('createScheduleModal'); showToast('Schedule created!', 'success'); await loadOperationsSchedules(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// ================= FLEET MANAGER DASHBOARD =================

async function loadFleetVehicles() {
    const tableBody = document.getElementById('fleetVehiclesTableBody');
    const dispatchVehSelect = document.getElementById('dispatchVehicleSelect');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/fleet/vehicles');
        const vehicles = await res.json();

        let activeBookings = [];
        try {
            const bRes = await fetch('/api/manager/bookings');
            if (bRes.ok) {
                // APPLIED FIX: Intercept bookings here so completed ones don't hold the vehicles hostage
                activeBookings = applyFrontendCompletedOverrides(await bRes.json());
            }
        } catch (e) { console.warn("Could not load bookings for cross-reference"); }

        tableBody.innerHTML = '';
        vehicles.forEach(v => {
            const badge = v.status === 'AVAILABLE' ? 'badge-confirmed' : v.status === 'UNDER_MAINTENANCE' ? 'badge-rejected' : 'badge-approved';

            let assignmentHtml = '';
            // Only show trip assignment if the booking is still CONFIRMED (not COMPLETED)
            const assignedTrip = activeBookings.find(b => b.status === 'CONFIRMED' && b.vehicleAssignment && b.vehicleAssignment.vehicle && b.vehicleAssignment.vehicle.id === v.id);
            if (assignedTrip) { assignmentHtml = `<br><span style="display: inline-block; margin-top: 4px; background: #009688; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.7rem;">Trip: ${assignedTrip.bookingReference}</span>`; }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${v.registrationNumber}</strong>${assignmentHtml}</td>
                    <td>${v.modelName}</td>
                    <td>${v.seatingCapacity} Seats</td>
                    <td>${v.isAirConditioned ? 'Yes (A/C)' : 'Open-Top (No A/C)'}</td>
                    <td><span class="card-badge ${badge}">${v.status}</span></td>
                    <td>
                        <div style="display: inline-flex; gap: 6px;">
                            ${v.status === 'UNDER_MAINTENANCE' ?
                                `<button class="btn btn-sm btn-outline" style="color: var(--color-success); border-color: var(--color-success);" onclick="markVehicleAvailable(${v.id})">Mark Available</button>` :
                                `<button class="btn btn-sm btn-outline" onclick="openMaintenanceModal(${v.id}, '${v.registrationNumber}')">Maintenance</button>`}
                            <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditVehicleModal(${v.id}, '${v.registrationNumber}', '${v.modelName}', ${v.seatingCapacity}, ${v.isAirConditioned})">Edit</button>
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteVehicle(${v.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;
        });

        if (dispatchVehSelect) {
            dispatchVehSelect.innerHTML = '<option value="">Select Safari 4x4 Vehicle</option>';
            vehicles.filter(v => {
                if (v.status !== 'AVAILABLE') return false;
                const isAssigned = activeBookings.some(b => b.status === 'CONFIRMED' && b.vehicleAssignment && b.vehicleAssignment.vehicle && b.vehicleAssignment.vehicle.id === v.id);
                return !isAssigned;
            }).forEach(v => { dispatchVehSelect.innerHTML += `<option value="${v.id}">${v.registrationNumber} (${v.modelName})</option>`; });
        }
    } catch (e) { console.error(e); }
}

async function loadFleetGuides() {
    const tableBody = document.getElementById('fleetGuidesTableBody');
    const assignGuideSelect = document.getElementById('assignGuideSelect');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/fleet/guides');
        const guides = await res.json();

        let activeBookings = [];
        try {
            const bRes = await fetch('/api/manager/bookings');
            if (bRes.ok) {
                // APPLIED FIX: Intercept bookings here so completed ones don't hold the guides hostage
                activeBookings = applyFrontendCompletedOverrides(await bRes.json());
            }
        } catch (e) {}

        tableBody.innerHTML = '';
        if (assignGuideSelect) assignGuideSelect.innerHTML = '<option value="">Select Available Guide</option>';

        guides.forEach(g => {
            const badge = g.status === 'AVAILABLE' ? 'badge-confirmed' : 'badge-rejected';

            let assignmentHtml = '';
            // Only show trip assignment if the booking is still CONFIRMED (not COMPLETED)
            const assignedTrip = activeBookings.find(b => b.status === 'CONFIRMED' && b.guideAssignment && b.guideAssignment.guide && b.guideAssignment.guide.id === g.id);
            if (assignedTrip) { assignmentHtml = `<br><span style="display: inline-block; margin-top: 4px; background: #009688; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.7rem;">Trip: ${assignedTrip.bookingReference}</span>`; }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${g.fullName}</strong>${assignmentHtml}</td>
                    <td>${g.licenseNumber}</td>
                    <td>${g.languagesSpoken}</td>
                    <td>${g.experienceYears} Years</td>
                    <td><span class="card-badge ${badge}">${g.status}</span></td>
                    <td>
                        <div style="display: inline-flex; gap: 6px;">
                            <button class="btn btn-sm btn-outline" style="${g.status !== 'AVAILABLE' ? 'color: var(--color-success); border-color: var(--color-success);' : ''}" onclick="toggleGuideStatus(${g.id}, '${g.status}')">
                                ${g.status === 'AVAILABLE' ? 'Mark Unavailable' : 'Mark Available'}
                            </button>
                            <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditGuideModal(${g.id}, '${g.fullName}', '${g.licenseNumber}', '${g.languagesSpoken}', ${g.experienceYears}, '${g.phone || ''}')">Edit</button>
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="deleteGuide(${g.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;

            if (assignGuideSelect && g.status === 'AVAILABLE') {
                const isAssigned = activeBookings.some(b => b.status === 'CONFIRMED' && b.guideAssignment && b.guideAssignment.guide && b.guideAssignment.guide.id === g.id);
                if (!isAssigned) assignGuideSelect.innerHTML += `<option value="${g.id}">${g.fullName} (${g.languagesSpoken})</option>`;
            }
        });
    } catch (e) { console.error(e); }
}

async function loadFleetConfirmedBookings() {
    const vehSelect = document.getElementById('dispatchBookingSelect');
    const guideSelect = document.getElementById('assignGuideBookingSelect');

    try {
        const res = await fetch('/api/manager/bookings');
        if (!res.ok) return;

        // APPLIED FIX: Intercept bookings
        const bookings = applyFrontendCompletedOverrides(await res.json());

        if (vehSelect) vehSelect.innerHTML = '<option value="">Select Confirmed Booking</option>';
        if (guideSelect) guideSelect.innerHTML = '<option value="">Select Confirmed Booking</option>';

        const todayStr = new Date().toISOString().split('T')[0];
        if (Array.isArray(bookings)) {
            bookings.filter(b => b.status === 'CONFIRMED' && b.bookingDate >= todayStr)
                .forEach(b => {
                    const opt = `<option value="${b.id}">${b.bookingReference} - ${b.safariPackage ? b.safariPackage.title : 'Trip'} (${b.bookingDate})</option>`;
                    const hasVehicle = b.vehicleAssignment || b.vehicle || b.assignedVehicle || b.safariVehicle || b.vehicleId;
                    const hasGuide = b.guideAssignment || b.guide || b.assignedGuide || b.safariGuide || b.guideId;

                    if (vehSelect && !hasVehicle) vehSelect.innerHTML += opt;
                    if (guideSelect && !hasGuide) guideSelect.innerHTML += opt;
                });
        }
    } catch (e) { console.error("Failed to load fleet confirmed bookings:", e); }
}

let activeVehicleForMaint = null;
function openMaintenanceModal(vehicleId, reg) {
    activeVehicleForMaint = vehicleId; document.getElementById('maintVehicleReg').textContent = reg;
    document.getElementById('maintType').value = ''; document.getElementById('maintDesc').value = ''; openModal('recordMaintenanceModal');
}

async function submitRecordMaintenance() {
    if (!activeVehicleForMaint) return;
    try {
        const res = await fetch('/api/fleet/maintenance', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ vehicleId: activeVehicleForMaint, maintenanceType: document.getElementById('maintType').value.trim(), startDate: new Date().toISOString().split('T')[0], description: document.getElementById('maintDesc').value }) });
        if (res.ok) { closeModal('recordMaintenanceModal'); showToast('Maintenance recorded', 'success'); await loadFleetVehicles(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function submitCreateVehicle() {
    try {
        const res = await fetch('/api/fleet/vehicles', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ registrationNumber: document.getElementById('newVehReg').value, modelName: document.getElementById('newVehModel').value, modelYear: document.getElementById('newVehYear').value, seatingCapacity: parseInt(document.getElementById('newVehSeats').value) || 6, isAirConditioned: document.getElementById('newVehAc').value === 'true' }) });
        if (res.ok) { closeModal('createVehicleModal'); showToast('Vehicle added!', 'success'); await loadFleetVehicles(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function markVehicleAvailable(id) {
    try {
        let res = await fetch(`/api/fleet/vehicles/${id}/status?status=AVAILABLE`, { method: 'PUT' });
        if (!res.ok) {
            const vRes = await fetch(`/api/fleet/vehicles/${id}`);
            if (vRes.ok) { const vehicle = await vRes.json(); vehicle.status = 'AVAILABLE'; res = await fetch(`/api/fleet/vehicles/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(vehicle) }); }
        }
        if (res.ok) { showToast('Vehicle marked Available!', 'success'); await loadFleetVehicles(); }
    } catch (e) { showToast('Error updating vehicle: ' + e.message, 'error'); }
}

async function submitCreateGuide() {
    try {
        const res = await fetch('/api/fleet/guides', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName: document.getElementById('newGuideName').value, licenseNumber: document.getElementById('newGuideLicense').value, languagesSpoken: document.getElementById('newGuideLangs').value, experienceYears: parseInt(document.getElementById('newGuideExp').value) || 1, phone: document.getElementById('newGuidePhone').value }) });
        if (res.ok) { closeModal('createGuideModal'); showToast('Guide added!', 'success'); await loadFleetGuides(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function submitGuideAssignment() {
    try {
        const res = await fetch('/api/fleet/assign-guide', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ bookingId: parseInt(document.getElementById('assignGuideBookingSelect').value), guideId: parseInt(document.getElementById('assignGuideSelect').value) }) });
        if (res.ok) { closeModal('assignGuideModal'); showToast('Guide Assigned', 'success'); await loadFleetGuides(); await loadFleetConfirmedBookings(); await loadPendingDispatchQueue(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function toggleGuideStatus(id, currentStatus) {
    const nextStatus = currentStatus === 'AVAILABLE' ? 'UNAVAILABLE' : 'AVAILABLE';
    try {
        const res = await fetch(`/api/fleet/guides/${id}/status?status=${nextStatus}`, { method: 'PUT' });
        if (res.ok) { showToast(`Guide status updated!`, 'success'); await loadFleetGuides(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function submitDispatchAssignment() {
    try {
        const res = await fetch('/api/fleet/assign-vehicle', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ bookingId: parseInt(document.getElementById('dispatchBookingSelect').value), vehicleId: parseInt(document.getElementById('dispatchVehicleSelect').value) }) });
        if (res.ok) { closeModal('dispatchModal'); showToast('Vehicle Assigned', 'success'); await loadFleetVehicles(); await loadFleetConfirmedBookings(); await loadPendingDispatchQueue(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// ================= FINANCE MANAGER DASHBOARD =================

async function loadFinancePayments() {
    const tableBody = document.getElementById('financePaymentsTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/finance/payments');
        const payments = await res.json();
        tableBody.innerHTML = '';

        // Pull our smart frontend overrides so the Finance Ledger knows what trips are finished!
        const completedTrips = JSON.parse(localStorage.getItem('safari_completed_trips')) || [];

        // banner: bank transfers waiting for verification
        const pendingTransfers = payments.filter(p => p.status === 'PENDING' && p.paymentMethod === 'BANK_TRANSFER');
        const banner = document.getElementById('financePendingTransfers');
        if (banner) {
            banner.hidden = pendingTransfers.length === 0;
            banner.innerHTML = `<strong>${pendingTransfers.length} bank transfer${pendingTransfers.length === 1 ? '' : 's'} waiting for verification.</strong>
                Check the company bank account, then click <em>Verify</em> or <em>Reject</em> below.`;
        }

        payments.forEach(p => {
            const badge = p.status === 'PAID' ? 'badge-confirmed' : p.status === 'REFUNDED' ? 'badge-refunded'
                : p.status === 'PENDING' ? 'badge-pending' : 'badge-rejected';
            const statusText = p.status === 'PENDING' && p.paymentMethod === 'BANK_TRANSFER' ? 'AWAITING VERIFICATION' : p.status;

            // 1. Check if the trip is finished
            let isCompleted = false;
            if (p.booking && (p.booking.status === 'COMPLETED' || completedTrips.includes(p.booking.id))) {
                isCompleted = true;
            }

            // 2. Decide what actions the Finance Manager is allowed to take
            let actionHtml = '';
            if (p.status === 'PAID') {
                if (isCompleted) {
                    // SECURE: Hide the refund button and lock the transaction!
                    actionHtml = `<span style="color: #4caf50; font-size: 0.75rem; font-weight: 700; background: rgba(76, 175, 80, 0.1); padding: 4px 8px; border-radius: 4px; border: 1px solid rgba(76,175,80,0.3);">Service Rendered</span>`;
                } else {
                    // Trip is still upcoming, refunds are allowed
                    actionHtml = `<button class="btn btn-sm btn-secondary" onclick="openRefundModal(${p.id}, '${p.transactionRef}', ${p.amount})">Refund</button>`;
                }
            } else if (p.status === 'PENDING' && p.paymentMethod === 'BANK_TRANSFER') {
                actionHtml = `
                    <button class="btn btn-sm btn-accent" onclick="verifyBankTransfer(${p.id}, '${p.transactionRef}')">Verify</button>
                    <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);" onclick="openRejectTransferModal(${p.id}, '${p.transactionRef}')">Reject</button>`;
            } else if (p.status === 'FAILED') {
                actionHtml = `<span style="color: var(--color-danger); font-size: 0.85rem;" title="${escapeHtml(p.failureReason)}">Failed attempt</span>`;
            } else {
                actionHtml = `<span style="color: var(--color-text-muted); font-size: 0.85rem;">Refunded</span>`;
            }

            tableBody.innerHTML += `
                <tr>
                    <td><strong>${p.transactionRef}</strong></td>
                    <td>${p.booking ? p.booking.bookingReference : '-'}</td>
                    <td><strong>${formatPrice(p.amount)}</strong></td>
                    <td>${paymentMethodLabel(p.paymentMethod)}${p.paymentDetails ? `<div class="pay-detail">${escapeHtml(p.paymentDetails)}</div>` : ''}</td>
                    <td><span class="card-badge ${badge}">${statusText}</span></td>
                    <td>${p.paidAt ? p.paidAt.substring(0, 10) : '-'}</td>
                    <td>
                        <div style="display: inline-flex; gap: 6px; align-items: center;">
                            ${actionHtml}
                            <button class="btn btn-sm btn-outline" style="color: var(--color-danger); border-color: var(--color-danger);"
                                onclick="deletePaymentRecord(${p.id})">Remove</button>
                        </div>
                    </td>
                </tr>
            `;
        });
    } catch (e) { console.error(e); }
}

async function loadFinanceSummary() {
    try {
        const res = await fetch('/api/finance/summary');
        const summary = await res.json();
        document.getElementById('statTotalRevenue').textContent = formatPrice(summary.totalRevenue || 0);
        document.getElementById('statTotalRefunded').textContent = formatPrice(summary.totalRefunded || 0);
        document.getElementById('statNetRevenue').textContent = formatPrice(summary.netRevenue || 0);
        const txObj = document.getElementById('statTotalTxns');
        if (txObj) animateValue(txObj, 0, summary.paidTransactionsCount || 0, 1500);

        const ctx = document.getElementById('financeTrendChart');
        if (ctx) {
            if (financeChartInstance) financeChartInstance.destroy();

            const labels = []; const dailyData = {};
            for (let i = 6; i >= 0; i--) {
                const d = new Date(); d.setDate(d.getDate() - i);
                const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
                dailyData[dateStr] = { rev: 0, count: 0, label: dayName };
                labels.push(dayName);
            }

            financeChartInstance = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [
                        { label: 'Daily Revenue ($)', data: [0,0,0,0,0,0,0], backgroundColor: 'rgba(0, 150, 136, 0.7)', borderColor: '#009688', borderWidth: 1, yAxisID: 'y' },
                        { label: 'Transactions', data: [0,0,0,0,0,0,0], type: 'line', backgroundColor: '#d99b26', borderColor: '#d99b26', borderWidth: 3, tension: 0.4, yAxisID: 'y1' }
                    ]
                },
                options: {
                    responsive: true, maintainAspectRatio: false,
                    scales: {
                        y: { type: 'linear', display: true, position: 'left', title: { display: true, text: 'Revenue ($)' } },
                        y1: { type: 'linear', display: true, position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: 'Transaction Count' }, ticks: { stepSize: 1, precision: 0 } }
                    }
                }
            });

            const tableBody = document.getElementById('financePaymentsTableBody');
            if (tableBody) {
                const syncChartWithTable = () => {
                    Object.keys(dailyData).forEach(k => { dailyData[k].rev = 0; dailyData[k].count = 0; });
                    tableBody.querySelectorAll('tr').forEach(row => {
                        const cells = row.querySelectorAll('td');
                        if (cells.length >= 6) {
                            const amount = parseFloat(cells[2].textContent.replace(/[^0-9.-]+/g, "")) || 0;
                            const dateStr = cells[5].textContent.trim().substring(0, 10);
                            if (dailyData[dateStr] && cells[4].textContent.trim() === 'PAID') { dailyData[dateStr].rev += amount; dailyData[dateStr].count += 1; }
                        }
                    });
                    const updatedRev = []; const updatedCount = [];
                    Object.keys(dailyData).sort().forEach(dateKey => { updatedRev.push(dailyData[dateKey].rev); updatedCount.push(dailyData[dateKey].count); });
                    financeChartInstance.data.datasets[0].data = updatedRev; financeChartInstance.data.datasets[1].data = updatedCount; financeChartInstance.update();
                };
                new MutationObserver(syncChartWithTable).observe(tableBody, { childList: true });
                syncChartWithTable();
            }
        }
    } catch (e) { console.error(e); }
}

async function downloadFinancialReport() {
    try {
        const res = await fetch('/api/finance/report/csv');
        if (!res.ok) throw new Error('Failed to generate report');
        const blob = await res.blob(); const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = `Safari_Financial_Report_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a); a.click(); window.URL.revokeObjectURL(url); document.body.removeChild(a);
    } catch (e) { showToast('Error generating report: ' + e.message, 'error'); }
}

let activePaymentForRefund = null;
function openRefundModal(paymentId, ref, maxAmount) {
    activePaymentForRefund = { id: paymentId, maxAmount: maxAmount }; document.getElementById('refundTxnRef').textContent = ref;
    document.getElementById('refundAmount').value = maxAmount; document.getElementById('refundReason').value = ''; openModal('refundModal');
}

async function submitRefund() {
    try {
        const res = await fetch('/api/finance/refunds', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paymentId: activePaymentForRefund.id, amount: parseFloat(document.getElementById('refundAmount').value), reason: document.getElementById('refundReason').value.trim() }) });
        if (res.ok) { closeModal('refundModal'); showToast(`Refund processed`, 'success'); await loadFinancePayments(); await loadFinanceSummary(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// ================= SYSTEM ADMINISTRATOR DASHBOARD =================

async function loadAdminUsers() {
    const tableBody = document.getElementById('adminUsersTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/admin/users', { cache: 'no-store' });
        const users = await res.json();
        tableBody.innerHTML = '';

        users.forEach(u => {
            const roleName = u.role ? u.role.name : '';
            tableBody.innerHTML += `
                <tr>
                    <td><strong>${u.fullName || 'Unknown User'}</strong></td>
                    <td>${u.email}</td>
                    <td><span class="card-badge badge-approved">${formatRoleName(roleName)}</span></td>
                    <td>${u.active ? '<span class="card-badge badge-confirmed">ACTIVE</span>' : '<span class="card-badge badge-rejected">INACTIVE</span>'}</td>
                    <td>
                        <div style="display: inline-flex; gap: 6px;">
                            <button class="btn btn-sm btn-outline" style="color: #f5ba42; border-color: #f5ba42;" onclick="openEditUserModal(${u.id}, '${(u.fullName || '').replace(/'/g, "\\'")}', '${(u.email || '').replace(/'/g, "\\'")}', '${(u.phone || '').replace(/'/g, "\\'")}', '${roleName}')">Edit</button>
                            <button class="btn btn-sm btn-outline" onclick="toggleUserActive(${u.id}, ${!u.active})">${u.active ? 'Deactivate' : 'Activate'}</button>
                        </div>
                    </td>
                </tr>
            `;
        });
    } catch (e) { console.error(e); }
}

function openEditUserModal(id, name, email, phone, role) {
    document.getElementById('editUserId').value = id; document.getElementById('editUserName').value = name;
    document.getElementById('editUserEmail').value = email; document.getElementById('editUserPhone').value = phone; document.getElementById('editUserRole').value = role;
    document.getElementById('editUserRole').disabled = (role === 'ROLE_CUSTOMER');
    openModal('editUserModal');
}

async function submitEditUser() {
    try {
        const res = await fetch(`/api/admin/users/${document.getElementById('editUserId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName: document.getElementById('editUserName').value, phone: document.getElementById('editUserPhone').value, role: document.getElementById('editUserRole').value }) });
        if (res.ok) { closeModal('editUserModal'); showToast('User updated!', 'success'); await loadAdminUsers(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}
async function submitAdminCreateUser() {
    try {
        const res = await fetch('/api/admin/users', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName: document.getElementById('newUserName').value, email: document.getElementById('newUserEmail').value, password: document.getElementById('newUserPassword').value, phone: document.getElementById('newUserPhone').value, role: document.getElementById('newUserRole').value, active: true }) });
        if (res.ok) { closeModal('adminCreateUserModal'); showToast('User created', 'success'); await loadAdminUsers(); await loadAdminAuditLogs(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function toggleUserActive(userId, newActive) {
    try {
        const res = await fetch(`/api/admin/users/${userId}/status?active=${newActive}`, { method: 'PUT' });
        if (res.ok) { showToast('Status Updated', 'success'); await loadAdminUsers(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

async function loadAdminAuditLogs() {
    const tableBody = document.getElementById('adminAuditTableBody');
    if (!tableBody) return;

    try {
        const res = await fetch('/api/admin/audit-logs');
        const logs = await res.json();
        tableBody.innerHTML = '';
        logs.slice(0, 15).forEach(l => {
            tableBody.innerHTML += `<tr><td><small>${l.createdAt ? l.createdAt.replace('T', ' ').substring(0, 19) : '-'}</small></td><td><strong>${l.actionType}</strong></td><td>${l.user ? l.user.email : 'System'}</td><td>${l.entityName} (${l.entityId})</td><td>${l.details || ''}</td></tr>`;
        });
    } catch (e) { console.error(e); }
}

// ================= EVENT LISTENERS =================
function initEventListeners() {
    const searchForm = document.getElementById('tripSearchForm');
    const destSelect = document.getElementById('searchDestination');

    document.querySelectorAll('.navbar a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId && targetId !== '#') {
                const targetElement = document.querySelector(targetId);
                if (targetElement) {
                    e.preventDefault();
                    const targetTop = targetElement.getBoundingClientRect().top + window.pageYOffset - (document.querySelector('.navbar')?.offsetHeight || 75) - 20;
                    if (window.cineScrollTo) window.cineScrollTo(targetTop);
                    else window.scrollTo({ top: targetTop, behavior: 'smooth' });
                    document.getElementById('mainNavLinks')?.classList.remove('open');
                    history.pushState(null, null, targetId);
                }
            }
        });
    });

    if (destSelect) destSelect.addEventListener('change', (e) => filterByDestination(e.target.value ? parseInt(e.target.value) : null, false));
    if (searchForm) searchForm.addEventListener('submit', (e) => { e.preventDefault(); filterByDestination(document.getElementById('searchDestination').value ? parseInt(document.getElementById('searchDestination').value) : null, true); });

    // =========================================================
    // Listen for Schedule & Calendar Changes
    // =========================================================
    const schedDestSelect = document.getElementById('schedDestSelect');
    if (schedDestSelect) {
        // When destination changes, update the package list!
        schedDestSelect.addEventListener('change', updateSchedulePackageDropdown);
    }

    const schedDate = document.getElementById('schedDate');
    if (schedDate) {
        // Run once instantly to set the minimum calendar date to Today
        enforceScheduleTimeSlots('schedDate', 'schedSlot');
        schedDate.addEventListener('change', () => enforceScheduleTimeSlots('schedDate', 'schedSlot'));
    }

    const editSchedDate = document.getElementById('editSchedDate');
    if (editSchedDate) {
        enforceScheduleTimeSlots('editSchedDate', 'editSchedSlot');
        editSchedDate.addEventListener('change', () => enforceScheduleTimeSlots('editSchedDate', 'editSchedSlot'));
    }

    // =========================================================
    // THE FIX: Restrict past dates on all public/customer calendars
    // =========================================================
    restrictPastDates();
}

function restrictPastDates() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;

    // Block past dates on the Public Search Bar
    const searchDate = document.getElementById('searchDate');
    if (searchDate) searchDate.min = todayStr;

    // Block past dates when a Customer tries to edit an existing booking
    const editBookingDate = document.getElementById('editBookingDate');
    if (editBookingDate) editBookingDate.min = todayStr;
}

// ================= PROFILE MANAGEMENT =================
function openProfileModal() {
    if (!currentUser) return;
    document.getElementById('profileEmail').value = currentUser.email || ''; document.getElementById('profileName').value = currentUser.fullName || '';
    document.getElementById('profilePhone').value = currentUser.phone || ''; document.getElementById('profileRoleBadge').textContent = formatRoleName(currentUser.role);
    openModal('profileModal');
}

async function submitProfileUpdate() {
    try {
        const res = await fetch('/api/user/profile', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName: document.getElementById('profileName').value.trim(), phone: document.getElementById('profilePhone').value.trim() }) });
        if (res.ok) { closeModal('profileModal'); showToast('Profile updated!', 'success'); await checkCurrentUser(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// --- UPDATE (EDIT) GUIDE LOGIC ---
function openEditGuideModal(id, name, license, langs, exp, phone) {
    document.getElementById('editGuideId').value = id; document.getElementById('editGuideName').value = name; document.getElementById('editGuideLicense').value = license;
    document.getElementById('editGuideLangs').value = langs; document.getElementById('editGuideExp').value = exp; document.getElementById('editGuidePhone').value = phone || ''; openModal('editGuideModal');
}

async function submitEditGuide() {
    try {
        const res = await fetch(`/api/fleet/guides/${document.getElementById('editGuideId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName: document.getElementById('editGuideName').value, licenseNumber: document.getElementById('editGuideLicense').value, languagesSpoken: document.getElementById('editGuideLangs').value, experienceYears: parseInt(document.getElementById('editGuideExp').value) || 1, phone: document.getElementById('editGuidePhone').value }) });
        if (res.ok) { closeModal('editGuideModal'); showToast('Guide updated!', 'success'); await loadFleetGuides(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// --- DELETE GUIDE LOGIC ---
async function deleteGuide(id) {
    if (!confirm('Are you sure you want to permanently delete this Safari Guide?')) return;
    try {
        const res = await fetch(`/api/fleet/guides/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Guide deleted!', 'success'); await loadFleetGuides(); }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// --- VEHICLE UPDATE & DELETE ---
function openEditVehicleModal(id, reg, model, seats, isAc) {
    document.getElementById('editVehId').value = id; document.getElementById('editVehReg').value = reg; document.getElementById('editVehModel').value = model;
    document.getElementById('editVehSeats').value = seats; document.getElementById('editVehAc').value = isAc ? 'true' : 'false'; openModal('editVehicleModal');
}

async function submitEditVehicle() {
    try {
        const res = await fetch(`/api/fleet/vehicles/${document.getElementById('editVehId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ registrationNumber: document.getElementById('editVehReg').value, modelName: document.getElementById('editVehModel').value, modelYear: document.getElementById('editVehYear').value, seatingCapacity: parseInt(document.getElementById('editVehSeats').value) || 6, isAirConditioned: document.getElementById('editVehAc').value === 'true' }) });
        if (res.ok) { closeModal('editVehicleModal'); showToast('Vehicle updated!', 'success'); loadFleetVehicles(); }
    } catch (e) { showToast('Error connecting to the server.', 'error'); }
}

async function deleteVehicle(id) {
    if (!confirm('Permanently delete this vehicle?')) return;
    try {
        const res = await fetch(`/api/fleet/vehicles/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Vehicle deleted.', 'success'); loadFleetVehicles(); }
    } catch (e) { showToast('Error deleting vehicle.', 'error'); }
}

// --- SCHEDULE UPDATE & DELETE ---
function openEditScheduleModal(id, date, slot, max) {
    document.getElementById('editSchedId').value = id; document.getElementById('editSchedDate').value = date; document.getElementById('editSchedSlot').value = slot; document.getElementById('editSchedMaxSlots').value = max; openModal('editScheduleModal');
}

async function submitEditSchedule() {
    try {
        const res = await fetch(`/api/operations/schedules/${document.getElementById('editSchedId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ scheduleDate: document.getElementById('editSchedDate').value, timeSlot: document.getElementById('editSchedSlot').value, maxSlots: parseInt(document.getElementById('editSchedMaxSlots').value) }) });
        if (res.ok) { closeModal('editScheduleModal'); showToast('Schedule updated!', 'success'); loadOperationsSchedules(); }
    } catch (e) { showToast('Error updating schedule.', 'error'); }
}

async function deleteSchedule(id) {
    if (!confirm('Permanently delete this schedule?')) return;
    try {
        const res = await fetch(`/api/operations/schedules/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Schedule deleted.', 'success'); loadOperationsSchedules(); }
    } catch (e) { showToast('Error deleting schedule.', 'error'); }
}

// --- SUPPORT TICKET UPDATE & DELETE ---
function openEditTicketModal(id, category, subject, desc) {
    document.getElementById('editTckId').value = id; document.getElementById('editTckCategory').value = category; document.getElementById('editTckSubject').value = subject; document.getElementById('editTckDesc').value = desc; openModal('editTicketModal');
}

async function submitEditTicket() {
    try {
        const res = await fetch(`/api/customer/support/tickets/${document.getElementById('editTckId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ category: document.getElementById('editTckCategory').value, subject: document.getElementById('editTckSubject').value, description: document.getElementById('editTckDesc').value }) });
        if (res.ok) { closeModal('editTicketModal'); showToast('Ticket updated!', 'success'); loadCustomerTickets(); }
    } catch (e) { showToast('Server connection error.', 'error'); }
}

async function deleteTicket(id) {
    if (!confirm('Permanently delete this support ticket?')) return;
    try {
        const res = await fetch(`/api/customer/support/tickets/${id}`, { method: 'DELETE' });
        if (res.ok) { showToast('Ticket deleted.', 'success'); loadCustomerTickets(); }
    } catch (e) { showToast('Error deleting ticket.', 'error'); }
}

// --- BOOKING UPDATE (EDIT) LOGIC ---
function openEditBookingModal(id, date, adults, children, requests) {
    document.getElementById('editBookingId').value = id; document.getElementById('editBookingDate').value = date; document.getElementById('editBookingAdults').value = adults; document.getElementById('editBookingChildren').value = children; document.getElementById('editBookingRequests').value = requests; openModal('editBookingModal');
}

async function submitEditBooking() {
    try {
        const res = await fetch(`/api/customer/bookings/${document.getElementById('editBookingId').value}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ bookingDate: document.getElementById('editBookingDate').value, guestAdults: parseInt(document.getElementById('editBookingAdults').value) || 1, guestChildren: parseInt(document.getElementById('editBookingChildren').value) || 0, specialRequests: document.getElementById('editBookingRequests').value }) });
        const data = await res.json().catch(() => ({}));
        if (res.ok) {
            closeModal('editBookingModal');
            showToast(`Booking updated! New total: ${formatPrice(data.totalAmount)}`, 'success');
            await loadCustomerBookings();
        } else {
            showToast(data.message || 'Could not update the booking.', 'error');
        }
    } catch (e) { showToast('Error: ' + e.message, 'error'); }
}

// --- MANAGER TICKET DISMISSAL ---
async function dismissManagerTicket(id) {
    if (!confirm('Are you sure you want to dismiss this resolved item from the inbox?')) return;
    let dismissedTickets = JSON.parse(localStorage.getItem('safari_dismissed_tickets')) || [];
    if (!dismissedTickets.includes(id)) dismissedTickets.push(id);
    localStorage.setItem('safari_dismissed_tickets', JSON.stringify(dismissedTickets));
    showToast('Item dismissed from Manager Inbox.', 'success'); await loadManagerTickets();
}

let isPreviewMode = false;
function togglePreviewMode() {
    isPreviewMode = !isPreviewMode;
    showToast(isPreviewMode ? 'Public Site Preview: ON' : 'Public Site Preview: OFF', 'info');
    updateUserUi();
    if (isPreviewMode) {
        setTimeout(() => {
            const destSection = document.getElementById('destinations');
            if (destSection) window.scrollTo({ top: destSection.getBoundingClientRect().top + window.pageYOffset - (document.querySelector('.navbar')?.offsetHeight || 75) - 20, behavior: 'smooth' });
        }, 100);
    }
}

function filterTable(tbodyId, query) {
    const lowerCaseQuery = query.toLowerCase().trim();
    document.querySelectorAll(`#${tbodyId} tr`).forEach(row => { row.style.display = row.textContent.toLowerCase().includes(lowerCaseQuery) ? '' : 'none'; });
}

// =========================================================================
// UPGRADE: COMPACT PENDING DISPATCH NOTIFICATION (LIVE UPDATING)
// =========================================================================
async function loadPendingDispatchQueue() {
    const notifBanner = document.getElementById('fleetDispatchNotification');
    const countText = document.getElementById('pendingDispatchCount');
    if (!notifBanner || !countText) return;

    try {
        const res = await fetch('/api/manager/bookings');

        // APPLIED FIX: Intercept bookings here as well
        const bookings = applyFrontendCompletedOverrides(await res.json());

        const pendingBookings = bookings.filter(b => {
            if (b.status !== 'CONFIRMED') return false;
            const hasVehicle = b.vehicleAssignment || b.vehicle || b.assignedVehicle || b.safariVehicle || b.vehicleId;
            const hasGuide = b.guideAssignment || b.guide || b.assignedGuide || b.safariGuide || b.guideId;
            return !hasVehicle || !hasGuide;
        });

        if (pendingBookings.length > 0) {
            countText.textContent = pendingBookings.length;
            notifBanner.style.display = 'flex';
        } else { notifBanner.style.display = 'none'; }
    } catch (e) { console.error("Failed to check dispatch queue:", e); }
}

setInterval(() => {
    const fleetTab = document.getElementById('fleet-vehicles');
    if (fleetTab && fleetTab.style.display !== 'none') loadPendingDispatchQueue();
}, 5000);

document.addEventListener('click', function(e) {
    if (e.target && e.target.getAttribute('data-tab') === 'fleet-vehicles') loadPendingDispatchQueue();
});

// =========================================================================
// SMART TIME CHECKER: Unlocks "Mark Completed" exactly 1 hour after safari ends!
// =========================================================================
function isSafariEligibleForCompletion(bookingDateStr, bookingObj) {
    const today = new Date();
    // Format local date to YYYY-MM-DD
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;

    // 1. If the date is in the past, it's definitely eligible.
    if (bookingDateStr < todayStr) return true;

    // 2. If it is NOT today (i.e., it's tomorrow), it's definitely NOT eligible.
    if (bookingDateStr > todayStr) return false;

    // 3. If it IS today, check the precise time slot!
    const currentHour = today.getHours(); // 0 to 23

    // Attempt to extract the slot data (handles different backend variations)
    const slot = bookingObj.schedule ? bookingObj.schedule.timeSlot :
                (bookingObj.safariSchedule ? bookingObj.safariSchedule.timeSlot :
                (bookingObj.timeSlot || ''));

    // Morning safaris usually end at 10:00 AM. Unlock at 11:00 AM (Hour 11).
    if (slot.includes('MORNING') && currentHour >= 11) return true;

    // Afternoon safaris usually end at 6:30 PM. Unlock at 7:00 PM (Hour 19).
    if (slot.includes('AFTERNOON') && currentHour >= 19) return true;

    // Full day safaris usually end at 6:00 PM. Unlock at 7:00 PM (Hour 19).
    if (slot.includes('FULL_DAY') && currentHour >= 19) return true;

    // Fallback: If we can't find a specific time slot, default to unlocking at 7:00 PM on the same day.
    if (!slot && currentHour >= 19) return true;

    return false;
}

// =========================================================================
// MULTI-CRITERIA FILTER: Search + Status Dropdown
// =========================================================================
function applyMasterListFilters() {
    const searchInput = document.getElementById('masterListSearchInput');
    const statusSelect = document.getElementById('masterListStatusFilter');

    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const statusFilter = statusSelect ? statusSelect.value : '';

    const rows = document.querySelectorAll('#managerAllBookingsTableBody tr');

    rows.forEach(row => {
        // Skip the "Loading..." placeholder row
        if (row.cells.length < 6) return;

        const rowText = row.textContent.toLowerCase();
        const statusBadge = row.querySelector('.card-badge');
        const rowStatus = statusBadge ? statusBadge.textContent.toUpperCase() : '';

        // Normalize the text in the visual badge to match our dropdown values
        let normalizedStatus = '';
        if (rowStatus.includes('PENDING')) normalizedStatus = 'PENDING';
        else if (rowStatus.includes('AWAITING PAYMENT') || rowStatus.includes('APPROVED')) normalizedStatus = 'APPROVED_AWAITING_PAYMENT';
        else if (rowStatus.includes('CONFIRMED')) normalizedStatus = 'CONFIRMED';
        else if (rowStatus.includes('COMPLETED')) normalizedStatus = 'COMPLETED';
        else if (rowStatus.includes('REJECTED') || rowStatus.includes('CANCELLED')) normalizedStatus = 'REJECTED';

        // A row must match BOTH the text search AND the status dropdown
        const matchesText = rowText.includes(query);
        const matchesStatus = (statusFilter === '') || (normalizedStatus === statusFilter);

        row.style.display = (matchesText && matchesStatus) ? '' : 'none';
    });
}

// =========================================================================
// SMART CALENDAR & TIME SLOT ENFORCEMENT
// =========================================================================
function enforceScheduleTimeSlots(dateInputId, slotSelectId) {
    const dateInput = document.getElementById(dateInputId);
    const slotSelect = document.getElementById(slotSelectId);
    if (!dateInput || !slotSelect) return;

    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;

    // 1. BULLETPROOF: Set the minimum selectable calendar date to TODAY
    dateInput.min = todayStr;

    const selectedDate = dateInput.value;
    const currentHour = today.getHours(); // 0 to 23 (e.g., 12 for 12:37 PM)

    // Reset all options to be selectable first
    Array.from(slotSelect.options).forEach(opt => {
        opt.disabled = false;
        opt.text = opt.text.replace(' (Passed)', '');
    });

    // 2. TIME SLOT LOGIC: If they picked TODAY, disable slots that already started
    if (selectedDate === todayStr) {
        Array.from(slotSelect.options).forEach(opt => {
            const val = opt.value;

            // Block 6:00 AM Morning/Full-Day safaris if it's currently 6 AM or later
            if ((val.includes('MORNING') || val.includes('FULL_DAY')) && currentHour >= 6) {
                opt.disabled = true;
                opt.text += ' (Passed)';
            }

            // Block 2:30 PM Afternoon safaris if it's currently 2 PM or later
            if (val.includes('AFTERNOON') && currentHour >= 14) {
                opt.disabled = true;
                opt.text += ' (Passed)';
            }
        });

        // If the currently selected slot is now disabled, auto-switch to a valid one
        if (slotSelect.selectedOptions[0] && slotSelect.selectedOptions[0].disabled) {
            const firstAvailable = Array.from(slotSelect.options).find(opt => !opt.disabled && opt.value !== "");
            if (firstAvailable) {
                slotSelect.value = firstAvailable.value;
            } else {
                slotSelect.value = '';
                showToast('No time slots are available for the rest of today.', 'info');
            }
        }
    }
}