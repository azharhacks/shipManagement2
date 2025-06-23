// API Base URL
const API_BASE_URL = 'http://localhost:4567/api';

// DOM Elements
const shipsContainer = document.getElementById('ships-container');
const addShipForm = document.getElementById('add-ship-form');

// Load all ships
async function loadShips() {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/ships`);
        const ships = await response.json();
        
        if (ships.length === 0) {
            shipsContainer.innerHTML = `
                <div class="col-span-3 text-center py-10">
                    <p class="text-gray-600 text-lg">No ships found. Add your first ship to get started!</p>
                </div>`;
            return;
        }
        
        renderShips(ships);
    } catch (error) {
        console.error('Error loading ships:', error);
        showError('Failed to load ships. Please try again later.');
    } finally {
        hideLoading();
    }
}

// Render ships in the UI
function renderShips(ships) {
    shipsContainer.innerHTML = ships.map(ship => `
        <div class="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow">
            <div class="flex justify-between items-start">
                <div>
                    <h3 class="text-xl font-semibold">${ship.name || 'Unnamed Ship'}</h3>
                    <p class="text-gray-600">${ship.type || 'Cargo'}</p>
                </div>
                <div class="text-2xl">
                    ${ship.type === 'Cargo' ? '🚢' : '🛳️'}
                </div>
            </div>
            <div class="mt-4 space-y-2">
                <p><i class="fas fa-map-marker-alt text-blue-500 mr-2"></i> ${ship.location || 'Unknown'}</p>
                <p><i class="fas fa-flag-checkered text-green-500 mr-2"></i> ${ship.destination || 'No destination'}</p>
                ${ship.type === 'Cargo' ? 
                    `<p><i class="fas fa-boxes text-yellow-500 mr-2"></i> Cargo: ${ship.cargo || '0'}</p>` :
                    `<p><i class="fas fa-users text-purple-500 mr-2"></i> Passengers: ${ship.passengers || '0'}</p>`
                }
            </div>
            <div class="mt-4 flex space-x-2">
                <button onclick="editShip('${ship.id}')" class="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors">
                    Edit
                </button>
                <button onclick="deleteShip('${ship.id}')" class="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 transition-colors">
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

// Add a new ship
async function addShip(shipData) {
    try {
        const response = await fetch(`${API_BASE_URL}/ships`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(shipData)
        });
        
        if (!response.ok) {
            throw new Error('Failed to add ship');
        }
        
        await loadShips();
        return { success: true };
    } catch (error) {
        console.error('Error adding ship:', error);
        return { success: false, error: error.message };
    }
}

// Delete a ship
async function deleteShip(shipId) {
    if (!confirm('Are you sure you want to delete this ship?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/ships/${shipId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete ship');
        }
        
        await loadShips();
    } catch (error) {
        console.error('Error deleting ship:', error);
        showError('Failed to delete ship. Please try again.');
    }
}

// Show loading state
function showLoading() {
    const loadingElement = document.getElementById('loading');
    if (loadingElement) loadingElement.classList.remove('hidden');
}

// Hide loading state
function hideLoading() {
    const loadingElement = document.getElementById('loading');
    if (loadingElement) loadingElement.classList.add('hidden');
}

// Show error message
function showError(message) {
    const errorElement = document.getElementById('error-message');
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.remove('hidden');
        setTimeout(() => errorElement.classList.add('hidden'), 5000);
    }
}

// Initialize the app
document.addEventListener('DOMContentLoaded', () => {
    loadShips();
    
    // Handle form submission
    if (addShipForm) {
        addShipForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const formData = new FormData(addShipForm);
            const shipData = {
                name: formData.get('name'),
                type: formData.get('type'),
                location: formData.get('location'),
                destination: formData.get('destination'),
                capacity: formData.get('capacity')
            };
            
            const result = await addShip(shipData);
            if (result.success) {
                addShipForm.reset();
                // Close modal if using one
                const modal = document.getElementById('add-ship-modal');
                if (modal) modal.classList.add('hidden');
            } else {
                showError(result.error || 'Failed to add ship');
            }
        });
    }
});
