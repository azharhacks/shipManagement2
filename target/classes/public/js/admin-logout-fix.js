// Direct fix for admin logout functionality
document.addEventListener('DOMContentLoaded', function() {
    console.log("Admin logout fix script loaded");
    
    // Find the logout link in the admin dashboard
    const logoutLink = document.querySelector('#logout-link');
    
    if (logoutLink) {
        console.log("Found logout link, adding event listener");
        
        // Remove any existing event listeners (in case they're causing conflicts)
        const newLogoutLink = logoutLink.cloneNode(true);
        logoutLink.parentNode.replaceChild(newLogoutLink, logoutLink);
        
        // Add our new event listener
        newLogoutLink.addEventListener('click', function(e) {
            e.preventDefault();
            console.log("Logout link clicked");
            
            // Call the logout API endpoint
            fetch('/api/logout')
                .then(response => {
                    console.log("Logout response:", response);
                    // Redirect to login page regardless of response
                    window.location.href = '/login.html';
                })
                .catch(error => {
                    console.error("Logout error:", error);
                    // Even if there's an error, redirect to login page
                    window.location.href = '/login.html';
                });
        });
    } else {
        console.warn("Logout link not found in the page");
    }
    
    // Also add a global click handler for any element with logout in the class or id
    document.addEventListener('click', function(e) {
        const target = e.target;
        
        // Check if the clicked element or any of its parents has logout in the id or class
        let currentElement = target;
        while (currentElement) {
            if (currentElement.id && currentElement.id.toLowerCase().includes('logout') ||
                currentElement.className && currentElement.className.toLowerCase().includes('logout')) {
                
                e.preventDefault();
                console.log("Logout element clicked via global handler");
                
                // Call the logout API endpoint
                fetch('/api/logout')
                    .then(response => {
                        console.log("Logout response:", response);
                        // Redirect to login page regardless of response
                        window.location.href = '/login.html';
                    })
                    .catch(error => {
                        console.error("Logout error:", error);
                        // Even if there's an error, redirect to login page
                        window.location.href = '/login.html';
                    });
                
                break;
            }
            currentElement = currentElement.parentElement;
        }
    });
});
