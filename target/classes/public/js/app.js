// API Base URL
const API_BASE_URL = 'http://localhost:8080/api';

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
                    <p class="text-gray-600">${ship.type || 'N/A'}</p>
                </div>
                <span class="px-3 py-1 rounded-full text-sm font-medium ${
                    ship.status === 'active' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-gray-100 text-gray-800'
                }">
                    ${ship.status || 'unknown'}
                </span>
            </div>
            <div class="mt-4 space-y-2">
                <p><span class="font-medium">IMO:</span> ${ship.imo || 'N/A'}</p>
                <p><span class="font-medium">Length:</span> ${ship.length || 'N/A'}m</p>
                <p><span class="font-medium">Beam:</span> ${ship.beam || 'N/A'}m</p>
                <p><span class="font-medium">Draft:</span> ${ship.draft || 'N/A'}m</p>
            </div>
            <div class="mt-4 flex space-x-2">
                <button onclick="editShip('${ship.id}')" class="text-blue-600 hover:text-blue-800">
                    Edit
                </button>
                <button onclick="deleteShip('${ship.id}')" class="text-red-600 hover:text-red-800">
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

// Show loading state
function showLoading() {
    // Add your loading state UI here
    if (shipsContainer) {
        shipsContainer.innerHTML = '<p>Loading ships...</p>';
    }
}

// Show error message
function showError(message) {
    if (shipsContainer) {
        shipsContainer.innerHTML = `
            <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
                <strong class="font-bold">Error: </strong>
                <span class="block sm:inline">${message}</span>
            </div>
        `;
    }
}

// Hide loading state
function hideLoading() {
    // Remove loading state UI here
}

// Initialize the app
document.addEventListener('DOMContentLoaded', () => {
    if (shipsContainer) {
        loadShips();
    }

    if (addShipForm) {
        addShipForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            // Handle form submission
        });
    }
});