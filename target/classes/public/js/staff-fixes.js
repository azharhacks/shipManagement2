// Staff Dashboard Fixes
document.addEventListener('DOMContentLoaded', function() {
    console.log('Staff fixes script loaded');
    
    // Fix navigation
    fixNavigation();
    
    // Fix sections loading
    fixSectionsLoading();
    
    // Fix crew management
    fixCrewManagement();
    
    // Fix reports
    fixReports();
    
    // Fix logout button
    fixLogoutButton();
});

// Fix navigation issues
function fixNavigation() {
    console.log('Fixing navigation');
    
    // Ensure navigation links work properly
    const navLinks = document.querySelectorAll('.sidebar-menu a');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Get the section ID from the href attribute
            const href = this.getAttribute('href');
            if (href && href.startsWith('#')) {
                const sectionId = href.substring(1);
                showSection(sectionId);
                
                // Update active link
                navLinks.forEach(l => l.classList.remove('active'));
                this.classList.add('active');
            }
        });
    });
}

// Fix sections loading
function fixSectionsLoading() {
    console.log('Fixing sections loading');
    
    // Fix overview section
    fixOverviewSection();
    
    // Fix ships section
    fixShipsSection();
    
    // Fix crew section
    fixCrewSection();
    
    // Fix reports section
    fixReportsSection();
}

// Fix overview section
function fixOverviewSection() {
    const overviewSection = document.getElementById('overview');
    if (overviewSection) {
        // Add demo data to overview section
        setTimeout(() => {
            // Check if data is already loaded
            const statsItems = overviewSection.querySelectorAll('.stats-item');
            if (statsItems.length === 0 || !statsItems[0].querySelector('.stats-value').textContent.trim()) {
                console.log('Adding demo data to overview section');
                
                // Add demo stats
                const statsContainer = overviewSection.querySelector('.stats-container');
                if (statsContainer) {
                    statsContainer.innerHTML = `
                        <div class="stats-item">
                            <div class="stats-icon">🚢</div>
                            <div class="stats-info">
                                <div class="stats-value">12</div>
                                <div class="stats-label">Total Ships</div>
                            </div>
                        </div>
                        <div class="stats-item">
                            <div class="stats-icon">👨‍✈️</div>
                            <div class="stats-info">
                                <div class="stats-value">45</div>
                                <div class="stats-label">Crew Members</div>
                            </div>
                        </div>
                        <div class="stats-item">
                            <div class="stats-icon">🔄</div>
                            <div class="stats-info">
                                <div class="stats-value">8</div>
                                <div class="stats-label">Active Voyages</div>
                            </div>
                        </div>
                        <div class="stats-item">
                            <div class="stats-icon">⚠️</div>
                            <div class="stats-info">
                                <div class="stats-value">3</div>
                                <div class="stats-label">Pending Issues</div>
                            </div>
                        </div>
                    `;
                }
                
                // Add recent activities
                const activitiesContainer = overviewSection.querySelector('.recent-activities');
                if (activitiesContainer) {
                    activitiesContainer.innerHTML = `
                        <h3>Recent Activities</h3>
                        <div class="activity-list">
                            <div class="activity-item">
                                <div class="activity-icon">🚢</div>
                                <div class="activity-details">
                                    <div class="activity-title">Ship "Northern Star" departed</div>
                                    <div class="activity-time">2 hours ago</div>
                                </div>
                            </div>
                            <div class="activity-item">
                                <div class="activity-icon">🔧</div>
                                <div class="activity-details">
                                    <div class="activity-title">Maintenance completed on "Ocean Explorer"</div>
                                    <div class="activity-time">5 hours ago</div>
                                </div>
                            </div>
                            <div class="activity-item">
                                <div class="activity-icon">📋</div>
                                <div class="activity-details">
                                    <div class="activity-title">New crew assignment for "Pacific Trader"</div>
                                    <div class="activity-time">Yesterday</div>
                                </div>
                            </div>
                            <div class="activity-item">
                                <div class="activity-icon">🚩</div>
                                <div class="activity-details">
                                    <div class="activity-title">Issue reported on "Coastal Voyager"</div>
                                    <div class="activity-time">2 days ago</div>
                                </div>
                            </div>
                        </div>
                    `;
                }
            }
        }, 1000);
    }
}

// Fix ships section
function fixShipsSection() {
    const shipsSection = document.getElementById('ships-section');
    if (shipsSection) {
        // Add demo data to ships section
        setTimeout(() => {
            // Check if data is already loaded
            const shipsList = shipsSection.querySelector('.ships-list');
            if (shipsList && (!shipsList.children || shipsList.children.length === 0)) {
                console.log('Adding demo data to ships section');
                
                // Add demo ships
                const demoShips = [
                    { id: 1, name: 'Ocean Explorer', type: 'Cargo Ship', status: 'Active', location: 'Port of Singapore' },
                    { id: 2, name: 'Coastal Voyager', type: 'Passenger Ferry', status: 'Maintenance', location: 'Port of Miami' },
                    { id: 3, name: 'Northern Star', type: 'Cruise Ship', status: 'En Route', location: 'Mediterranean Sea' },
                    { id: 4, name: 'Pacific Trader', type: 'Container Ship', status: 'Available', location: 'Port of Shanghai' },
                    { id: 5, name: 'Atlantic Pioneer', type: 'Research Vessel', status: 'Docked', location: 'Port of Rotterdam' }
                ];
                
                demoShips.forEach(ship => {
                    // Set appropriate status class
                    let statusClass = 'status-inactive';
                    if (ship.status === 'Active' || ship.status === 'Available') {
                        statusClass = 'status-active';
                    } else if (ship.status === 'Maintenance') {
                        statusClass = 'status-maintenance';
                    } else if (ship.status === 'En Route') {
                        statusClass = 'status-en-route';
                    }
                    
                    const shipItem = document.createElement('div');
                    shipItem.className = 'ship-item';
                    shipItem.innerHTML = `
                        <div class="ship-info">
                            <h3>${ship.name}</h3>
                            <p>${ship.type}</p>
                            <p>Location: ${ship.location}</p>
                        </div>
                        <div class="ship-status ${statusClass}">${ship.status}</div>
                        <div class="ship-actions">
                            <button class="btn view-ship" data-ship-id="${ship.id}">View Details</button>
                            <button class="btn edit-ship" data-ship-id="${ship.id}">Edit</button>
                        </div>
                    `;
                    
                    shipsList.appendChild(shipItem);
                });
                
                // Add event listeners to ship buttons
                const viewButtons = shipsList.querySelectorAll('.view-ship');
                viewButtons.forEach(button => {
                    button.addEventListener('click', function() {
                        const shipId = this.getAttribute('data-ship-id');
                        alert(`Viewing details for ship ID: ${shipId}`);
                    });
                });
                
                const editButtons = shipsList.querySelectorAll('.edit-ship');
                editButtons.forEach(button => {
                    button.addEventListener('click', function() {
                        const shipId = this.getAttribute('data-ship-id');
                        alert(`Editing ship ID: ${shipId}`);
                    });
                });
            }
        }, 1000);
    }
}

// Fix crew section
function fixCrewSection() {
    const crewSection = document.getElementById('crew-section');
    if (crewSection) {
        // Add demo data to crew section
        setTimeout(() => {
            // Check if data is already loaded
            const crewList = crewSection.querySelector('.crew-list');
            if (crewList && (!crewList.children || crewList.children.length === 0)) {
                console.log('Adding demo data to crew section');
                
                // Add demo crew members
                const demoCrewMembers = [
                    { id: 1, name: 'John Smith', role: 'Captain', ship: 'Ocean Explorer', status: 'On Duty' },
                    { id: 2, name: 'Maria Rodriguez', role: 'First Officer', ship: 'Ocean Explorer', status: 'On Duty' },
                    { id: 3, name: 'David Chen', role: 'Engineer', ship: 'Northern Star', status: 'On Leave' },
                    { id: 4, name: 'Sarah Johnson', role: 'Navigation Officer', ship: 'Pacific Trader', status: 'On Duty' },
                    { id: 5, name: 'Michael Brown', role: 'Security Officer', ship: 'Coastal Voyager', status: 'Training' }
                ];
                
                demoCrewMembers.forEach(crew => {
                    // Set appropriate status class
                    let statusClass = 'status-inactive';
                    if (crew.status === 'On Duty') {
                        statusClass = 'status-active';
                    } else if (crew.status === 'On Leave') {
                        statusClass = 'status-maintenance';
                    } else if (crew.status === 'Training') {
                        statusClass = 'status-en-route';
                    }
                    
                    const crewItem = document.createElement('div');
                    crewItem.className = 'crew-item';
                    crewItem.innerHTML = `
                        <div class="crew-info">
                            <h3>${crew.name}</h3>
                            <p>${crew.role}</p>
                            <p>Ship: ${crew.ship}</p>
                        </div>
                        <div class="crew-status ${statusClass}">${crew.status}</div>
                        <div class="crew-actions">
                            <button class="btn view-crew" data-crew-id="${crew.id}">View Details</button>
                            <button class="btn edit-crew" data-crew-id="${crew.id}">Edit</button>
                        </div>
                    `;
                    
                    crewList.appendChild(crewItem);
                });
                
                // Add event listeners to crew buttons
                const viewButtons = crewList.querySelectorAll('.view-crew');
                viewButtons.forEach(button => {
                    button.addEventListener('click', function() {
                        const crewId = this.getAttribute('data-crew-id');
                        alert(`Viewing details for crew member ID: ${crewId}`);
                    });
                });
                
                const editButtons = crewList.querySelectorAll('.edit-crew');
                editButtons.forEach(button => {
                    button.addEventListener('click', function() {
                        const crewId = this.getAttribute('data-crew-id');
                        alert(`Editing crew member ID: ${crewId}`);
                    });
                });
            }
        }, 1000);
    }
}

// Fix reports section
function fixReportsSection() {
    const reportsSection = document.getElementById('reports-section');
    if (reportsSection) {
        // Add demo data to reports section
        setTimeout(() => {
            // Check if data is already loaded
            const reportsList = reportsSection.querySelector('.reports-list');
            if (reportsList && (!reportsList.children || reportsList.children.length === 0)) {
                console.log('Adding demo data to reports section');
                
                // Create reports list if it doesn't exist
                if (!reportsList) {
                    const sectionContainer = reportsSection.querySelector('.section-container');
                    if (sectionContainer) {
                        const reportsListDiv = document.createElement('div');
                        reportsListDiv.className = 'reports-list';
                        sectionContainer.appendChild(reportsListDiv);
                    }
                }
                
                // Add demo reports
                const demoReports = [
                    { id: 1, title: 'Monthly Maintenance Report', date: '2025-07-01', status: 'Completed' },
                    { id: 2, title: 'Fuel Consumption Analysis', date: '2025-06-15', status: 'Pending Review' },
                    { id: 3, title: 'Crew Performance Evaluation', date: '2025-06-30', status: 'In Progress' },
                    { id: 4, title: 'Safety Inspection Results', date: '2025-07-05', status: 'Completed' }
                ];
                
                const reportsListElement = reportsList || reportsSection.querySelector('.reports-list');
                if (reportsListElement) {
                    demoReports.forEach(report => {
                        // Set appropriate status class
                        let statusClass = 'status-inactive';
                        if (report.status === 'Completed') {
                            statusClass = 'status-active';
                        } else if (report.status === 'In Progress') {
                            statusClass = 'status-en-route';
                        } else if (report.status === 'Pending Review') {
                            statusClass = 'status-maintenance';
                        }
                        
                        const reportItem = document.createElement('div');
                        reportItem.className = 'report-item';
                        reportItem.innerHTML = `
                            <div class="report-info">
                                <h3>${report.title}</h3>
                                <p>Date: ${report.date}</p>
                            </div>
                            <div class="report-status ${statusClass}">${report.status}</div>
                            <div class="report-actions">
                                <button class="btn view-report" data-report-id="${report.id}">View Report</button>
                                <button class="btn download-report" data-report-id="${report.id}">Download</button>
                            </div>
                        `;
                        
                        reportsListElement.appendChild(reportItem);
                    });
                    
                    // Add event listeners to report buttons
                    const viewButtons = reportsListElement.querySelectorAll('.view-report');
                    viewButtons.forEach(button => {
                        button.addEventListener('click', function() {
                            const reportId = this.getAttribute('data-report-id');
                            alert(`Viewing report ID: ${reportId}`);
                        });
                    });
                    
                    const downloadButtons = reportsListElement.querySelectorAll('.download-report');
                    downloadButtons.forEach(button => {
                        button.addEventListener('click', function() {
                            const reportId = this.getAttribute('data-report-id');
                            alert(`Downloading report ID: ${reportId}`);
                        });
                    });
                }
            }
        }, 1000);
    }
}

// Fix crew management
function fixCrewManagement() {
    // Add event listener to crew form
    const crewForm = document.getElementById('add-crew-form');
    if (crewForm) {
        crewForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form data
            const name = document.getElementById('crew-name').value;
            const role = document.getElementById('crew-role').value;
            const ship = document.getElementById('crew-ship').value;
            
            // Validate form
            if (!name || !role || !ship) {
                alert('Please fill in all required fields.');
                return;
            }
            
            // Add new crew member to list
            const crewList = document.querySelector('.crew-list');
            if (crewList) {
                const crewId = Math.floor(Math.random() * 1000);
                
                const crewItem = document.createElement('div');
                crewItem.className = 'crew-item';
                crewItem.innerHTML = `
                    <div class="crew-info">
                        <h3>${name}</h3>
                        <p>${role}</p>
                        <p>Ship: ${ship}</p>
                    </div>
                    <div class="crew-status status-active">On Duty</div>
                    <div class="crew-actions">
                        <button class="btn view-crew" data-crew-id="${crewId}">View Details</button>
                        <button class="btn edit-crew" data-crew-id="${crewId}">Edit</button>
                    </div>
                `;
                
                crewList.appendChild(crewItem);
                
                // Reset form
                crewForm.reset();
                
                // Show success message
                alert('Crew member added successfully!');
            }
        });
    }
}

// Fix reports
function fixReports() {
    // Add event listener to report form
    const reportForm = document.getElementById('add-report-form');
    if (reportForm) {
        reportForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form data
            const title = document.getElementById('report-title').value;
            const date = document.getElementById('report-date').value;
            const content = document.getElementById('report-content').value;
            
            // Validate form
            if (!title || !date || !content) {
                alert('Please fill in all required fields.');
                return;
            }
            
            // Add new report to list
            const reportsList = document.querySelector('.reports-list');
            if (reportsList) {
                const reportId = Math.floor(Math.random() * 1000);
                
                const reportItem = document.createElement('div');
                reportItem.className = 'report-item';
                reportItem.innerHTML = `
                    <div class="report-info">
                        <h3>${title}</h3>
                        <p>Date: ${date}</p>
                    </div>
                    <div class="report-status status-en-route">In Progress</div>
                    <div class="report-actions">
                        <button class="btn view-report" data-report-id="${reportId}">View Report</button>
                        <button class="btn download-report" data-report-id="${reportId}">Download</button>
                    </div>
                `;
                
                reportsList.appendChild(reportItem);
                
                // Reset form
                reportForm.reset();
                
                // Show success message
                alert('Report created successfully!');
            }
        });
    }
}

// Fix logout button
function fixLogoutButton() {
    console.log('Fixing logout button');
    
    // Find all logout buttons/links
    const logoutElements = document.querySelectorAll('.logout-btn, #logout-btn, .logout, #logout');
    
    logoutElements.forEach(element => {
        element.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Logout clicked');
            
            // Call logout API
            fetch('/api/logout')
                .then(response => {
                    console.log('Logout successful');
                    // Redirect to login page
                    window.location.href = '/login.html';
                })
                .catch(error => {
                    console.error('Logout error:', error);
                    // Redirect anyway
                    window.location.href = '/login.html';
                });
        });
    });
}
