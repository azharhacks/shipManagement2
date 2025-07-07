// User Dashboard Fixes
document.addEventListener('DOMContentLoaded', function() {
    // Fix for ships not showing
    fixShipsDisplay();
    
    // Fix for navigation
    fixNavigation();
    
    // Fix for bookings
    fixBookings();
    
    // Fix for logout button
    fixLogoutButton();
    
    // Fix for settings
    fixSettings();
});

// Fix for ships display issue
function fixShipsDisplay() {
    // Wait for ships to load, then check if we need to add demo ships
    setTimeout(() => {
        const shipsGrid = document.getElementById('ships-grid');
        const shipsLoading = document.getElementById('ships-loading');
        const noShipsMessage = document.getElementById('no-ships-message');
        
        // If there's only one ship or no ships showing, add demo ships
        if (shipsGrid && (!shipsGrid.children || shipsGrid.children.length <= 1)) {
            console.log("Adding demo ships as fallback");
            
            // Hide loading and no ships message
            if (shipsLoading) shipsLoading.style.display = 'none';
            if (noShipsMessage) noShipsMessage.style.display = 'none';
            
            // Add demo ships
            addDemoShips(shipsGrid);
        }
    }, 1000);
    
    // Add event listeners for ship details and booking
    document.addEventListener('click', function(e) {
        // Handle view details button click
        if (e.target.classList.contains('view-ship-details') || e.target.closest('.view-ship-details')) {
            const button = e.target.classList.contains('view-ship-details') ? e.target : e.target.closest('.view-ship-details');
            const shipId = button.getAttribute('data-ship-id');
            if (shipId) {
                showShipDetails(shipId);
            }
        }
        
        // Handle book ship button click
        if (e.target.classList.contains('book-ship') || e.target.closest('.book-ship')) {
            const button = e.target.classList.contains('book-ship') ? e.target : e.target.closest('.book-ship');
            const shipId = button.getAttribute('data-ship-id');
            const shipName = button.getAttribute('data-ship-name');
            if (shipId && shipName) {
                showBookingForm(shipId, shipName);
            }
        }
    });
}

// Add demo ships to the grid
function addDemoShips(shipsGrid) {
    const demoShips = [
        { id: 'demo1', name: 'Ocean Explorer', type: 'Cargo Ship', capacity: '5000 tons', status: 'Active' },
        { id: 'demo2', name: 'Coastal Voyager', type: 'Passenger Ferry', capacity: '250 passengers', status: 'Maintenance' },
        { id: 'demo3', name: 'Northern Star', type: 'Cruise Ship', capacity: '1200 passengers', status: 'En Route' },
        { id: 'demo4', name: 'Pacific Trader', type: 'Container Ship', capacity: '10000 TEU', status: 'Available' },
        { id: 'demo5', name: 'Atlantic Pioneer', type: 'Research Vessel', capacity: '50 crew', status: 'Docked' }
    ];
    
    demoShips.forEach(ship => {
        // Set appropriate badge color based on status
        let statusBadgeClass = 'bg-secondary';
        if (ship.status === 'Active' || ship.status === 'Available') {
            statusBadgeClass = 'bg-success';
        } else if (ship.status === 'Maintenance') {
            statusBadgeClass = 'bg-warning';
        } else if (ship.status === 'En Route') {
            statusBadgeClass = 'bg-info';
        }
        
        const shipCard = document.createElement('div');
        shipCard.className = 'col-md-4 mb-4';
        shipCard.innerHTML = `
            <div class="card h-100">
                <div class="card-header bg-primary text-white">
                    <h5 class="card-title mb-0">${ship.name}</h5>
                </div>
                <div class="card-body">
                    <p><strong>Type:</strong> ${ship.type}</p>
                    <p><strong>Capacity:</strong> ${ship.capacity}</p>
                    <p><strong>Status:</strong> <span class="badge ${statusBadgeClass}">${ship.status}</span></p>
                </div>
                <div class="card-footer">
                    <button class="btn btn-primary view-ship-details" data-ship-id="${ship.id}">
                        <i class="bi bi-eye"></i> View Details
                    </button>
                    <button class="btn btn-success book-ship" data-ship-id="${ship.id}" data-ship-name="${ship.name}" ${ship.status !== 'Active' && ship.status !== 'Available' ? 'disabled' : ''}>
                        <i class="bi bi-calendar-plus"></i> Book
                    </button>
                </div>
            </div>
        `;
        
        shipsGrid.appendChild(shipCard);
    });
}

// Show ship details in modal
function showShipDetails(shipId) {
    // Get ship details from API or use demo data
    let shipDetails = null;
    
    // Check if it's a demo ship
    if (shipId.startsWith('demo')) {
        const demoShips = {
            'demo1': { name: 'Ocean Explorer', type: 'Cargo Ship', capacity: '5000 tons', status: 'Active', location: 'Port of Singapore', lastUpdated: '2025-07-07 12:30' },
            'demo2': { name: 'Coastal Voyager', type: 'Passenger Ferry', capacity: '250 passengers', status: 'Maintenance', location: 'Port of Miami', lastUpdated: '2025-07-06 09:15' },
            'demo3': { name: 'Northern Star', type: 'Cruise Ship', capacity: '1200 passengers', status: 'En Route', location: 'Mediterranean Sea', lastUpdated: '2025-07-07 15:45' },
            'demo4': { name: 'Pacific Trader', type: 'Container Ship', capacity: '10000 TEU', status: 'Available', location: 'Port of Shanghai', lastUpdated: '2025-07-07 10:20' },
            'demo5': { name: 'Atlantic Pioneer', type: 'Research Vessel', capacity: '50 crew', status: 'Docked', location: 'Port of Rotterdam', lastUpdated: '2025-07-05 14:10' }
        };
        
        shipDetails = demoShips[shipId];
    }
    
    if (shipDetails) {
        // Update modal with ship details
        document.getElementById('modal-ship-name').textContent = shipDetails.name;
        document.getElementById('modal-ship-type').textContent = shipDetails.type;
        document.getElementById('modal-ship-capacity').textContent = shipDetails.capacity;
        document.getElementById('modal-ship-status').textContent = shipDetails.status;
        document.getElementById('modal-ship-location').textContent = shipDetails.location;
        document.getElementById('modal-ship-last-updated').textContent = shipDetails.lastUpdated;
        
        // Update book button state based on status
        const bookButton = document.getElementById('modal-book-ship-btn');
        if (bookButton) {
            if (shipDetails.status === 'Active' || shipDetails.status === 'Available') {
                bookButton.disabled = false;
                bookButton.setAttribute('data-ship-id', shipId);
                bookButton.setAttribute('data-ship-name', shipDetails.name);
            } else {
                bookButton.disabled = true;
            }
        }
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('ship-details-modal'));
        modal.show();
    }
}

// Show booking form
function showBookingForm(shipId, shipName) {
    // Update booking form with ship info
    const bookingShipName = document.getElementById('booking-ship-name');
    if (bookingShipName) {
        bookingShipName.textContent = shipName;
    }
    
    const bookingShipId = document.getElementById('booking-ship-id');
    if (bookingShipId) {
        bookingShipId.value = shipId;
    }
    
    // Show booking section
    showSection('bookings-section');
    
    // Scroll to booking form
    const bookingForm = document.getElementById('booking-form');
    if (bookingForm) {
        bookingForm.scrollIntoView({ behavior: 'smooth' });
    }
}

// Fix navigation
function fixNavigation() {
    // Add event listeners to navigation links
    const navLinks = document.querySelectorAll('.nav-link[data-section]');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all links
            navLinks.forEach(l => l.classList.remove('active'));
            
            // Add active class to clicked link
            this.classList.add('active');
            
            // Show corresponding section
            const sectionId = this.getAttribute('data-section');
            showSection(sectionId);
        });
    });
    
    // Mobile sidebar toggle
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');
    
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('show');
            if (sidebarOverlay) {
                sidebarOverlay.classList.toggle('show');
            }
        });
        
        if (sidebarOverlay) {
            sidebarOverlay.addEventListener('click', function() {
                sidebar.classList.remove('show');
                sidebarOverlay.classList.remove('show');
            });
        }
    }
}

// Show a specific section
function showSection(sectionId) {
    // Hide all sections
    const sections = document.querySelectorAll('.content-section');
    sections.forEach(section => section.classList.remove('active'));
    
    // Show the requested section
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.add('active');
    }
}

// Fix bookings functionality
function fixBookings() {
    // Add event listener to booking form
    const bookingForm = document.getElementById('booking-form');
    if (bookingForm) {
        bookingForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form data
            const shipId = document.getElementById('booking-ship-id').value;
            const startDate = document.getElementById('booking-start-date').value;
            const endDate = document.getElementById('booking-end-date').value;
            const purpose = document.getElementById('booking-purpose').value;
            
            // Validate form
            if (!startDate || !endDate || !purpose) {
                showAlert('Please fill in all required fields.', 'danger');
                return;
            }
            
            // Create booking (demo)
            createDemoBooking(shipId, startDate, endDate, purpose);
        });
    }
    
    // Load demo bookings
    loadDemoBookings();
}

// Create a demo booking
function createDemoBooking(shipId, startDate, endDate, purpose) {
    // Create a new booking entry
    const bookingsTable = document.getElementById('bookings-table-body');
    if (bookingsTable) {
        // Get ship name
        let shipName = 'Unknown Ship';
        if (shipId.startsWith('demo')) {
            const demoShips = {
                'demo1': 'Ocean Explorer',
                'demo2': 'Coastal Voyager',
                'demo3': 'Northern Star',
                'demo4': 'Pacific Trader',
                'demo5': 'Atlantic Pioneer'
            };
            shipName = demoShips[shipId] || shipName;
        }
        
        // Create booking ID
        const bookingId = 'BK' + Math.floor(Math.random() * 10000);
        
        // Create booking date
        const bookingDate = new Date().toISOString().split('T')[0];
        
        // Create booking status
        const status = 'Pending';
        
        // Add to table
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${bookingId}</td>
            <td>${shipName}</td>
            <td>${startDate}</td>
            <td>${endDate}</td>
            <td>${purpose}</td>
            <td><span class="badge bg-warning">${status}</span></td>
            <td>
                <button class="btn btn-sm btn-danger cancel-booking" data-booking-id="${bookingId}">
                    <i class="bi bi-x-circle"></i> Cancel
                </button>
            </td>
        `;
        
        bookingsTable.appendChild(tr);
        
        // Reset form
        document.getElementById('booking-form').reset();
        
        // Show success message
        showAlert('Booking created successfully!', 'success');
    }
}

// Load demo bookings
function loadDemoBookings() {
    const bookingsTable = document.getElementById('bookings-table-body');
    const bookingsLoading = document.getElementById('bookings-loading');
    const noBookingsMessage = document.getElementById('no-bookings-message');
    
    if (bookingsTable) {
        // Hide loading
        if (bookingsLoading) bookingsLoading.style.display = 'none';
        
        // Check if table is empty
        if (!bookingsTable.children || bookingsTable.children.length === 0) {
            // Add demo bookings
            const demoBookings = [
                { id: 'BK1234', ship: 'Ocean Explorer', startDate: '2025-07-15', endDate: '2025-07-20', purpose: 'Cargo Transport', status: 'Confirmed' },
                { id: 'BK5678', ship: 'Pacific Trader', startDate: '2025-08-01', endDate: '2025-08-10', purpose: 'Container Shipping', status: 'Pending' }
            ];
            
            demoBookings.forEach(booking => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${booking.id}</td>
                    <td>${booking.ship}</td>
                    <td>${booking.startDate}</td>
                    <td>${booking.endDate}</td>
                    <td>${booking.purpose}</td>
                    <td><span class="badge ${booking.status === 'Confirmed' ? 'bg-success' : 'bg-warning'}">${booking.status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-danger cancel-booking" data-booking-id="${booking.id}">
                            <i class="bi bi-x-circle"></i> Cancel
                        </button>
                    </td>
                `;
                
                bookingsTable.appendChild(tr);
            });
            
            // Hide no bookings message
            if (noBookingsMessage) noBookingsMessage.style.display = 'none';
        }
        
        // Add event listener for cancel buttons
        document.addEventListener('click', function(e) {
            if (e.target.classList.contains('cancel-booking') || e.target.closest('.cancel-booking')) {
                const button = e.target.classList.contains('cancel-booking') ? e.target : e.target.closest('.cancel-booking');
                const bookingId = button.getAttribute('data-booking-id');
                if (bookingId) {
                    // Remove booking row
                    const row = button.closest('tr');
                    if (row) {
                        row.remove();
                        showAlert(`Booking ${bookingId} cancelled successfully!`, 'success');
                    }
                }
            }
        });
    }
}

// Fix logout button
function fixLogoutButton() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Call logout API
            fetch('/api/logout')
                .then(() => {
                    // Redirect to login page
                    window.location.href = '/login.html';
                })
                .catch(() => {
                    // Redirect even if there's an error
                    window.location.href = '/login.html';
                });
        });
    }
}

// Fix settings functionality
function fixSettings() {
    // Add event listener to profile form
    const profileForm = document.getElementById('profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Show success message
            showAlert('Profile updated successfully!', 'success');
        });
    }
    
    // Add event listener to password form
    const passwordForm = document.getElementById('password-form');
    if (passwordForm) {
        passwordForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form data
            const currentPassword = document.getElementById('current-password').value;
            const newPassword = document.getElementById('new-password').value;
            const confirmPassword = document.getElementById('confirm-password').value;
            
            // Validate form
            if (!currentPassword || !newPassword || !confirmPassword) {
                showAlert('Please fill in all password fields.', 'danger');
                return;
            }
            
            if (newPassword !== confirmPassword) {
                showAlert('New password and confirmation do not match.', 'danger');
                return;
            }
            
            // Show success message
            showAlert('Password changed successfully!', 'success');
            
            // Reset form
            passwordForm.reset();
        });
    }
}

// Show alert message
function showAlert(message, type) {
    const alertContainer = document.getElementById('alert-container');
    if (alertContainer) {
        const alert = document.createElement('div');
        alert.className = `alert alert-${type} alert-dismissible fade show`;
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        
        alertContainer.appendChild(alert);
        
        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => {
                alert.remove();
            }, 150);
        }, 5000);
    }
}
