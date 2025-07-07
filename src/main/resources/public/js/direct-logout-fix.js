// Direct fix for admin logout button
(function() {
    console.log("Direct logout fix script loaded");
    
    // Function to run when DOM is loaded
    function fixLogoutButton() {
        console.log("Attempting to fix logout button");
        
        // Find the logout link element
        const logoutLinkItem = document.querySelector('li.nav-item a#logout-link');
        
        if (logoutLinkItem) {
            console.log("Found logout link, replacing with new element");
            
            // Get the parent li element
            const parentLi = logoutLinkItem.parentElement;
            
            // Create a completely new element
            const newLi = document.createElement('li');
            newLi.className = 'nav-item mt-4';
            newLi.innerHTML = `
                <a class="nav-link text-danger" href="/login.html" id="new-logout-link">
                    <i class="bi bi-box-arrow-right"></i> Logout
                </a>
            `;
            
            // Replace the old element with the new one
            if (parentLi && parentLi.parentElement) {
                parentLi.parentElement.replaceChild(newLi, parentLi);
                
                // Add click event listener to the new logout link
                document.getElementById('new-logout-link').addEventListener('click', function(e) {
                    e.preventDefault();
                    console.log("New logout link clicked");
                    
                    // Clear session on client side
                    if (window.sessionStorage) {
                        sessionStorage.clear();
                    }
                    if (window.localStorage) {
                        localStorage.removeItem('user');
                        localStorage.removeItem('token');
                    }
                    
                    // Call the logout API endpoint
                    fetch('/api/logout', {
                        method: 'GET',
                        credentials: 'same-origin'
                    })
                    .then(response => {
                        console.log("Logout response:", response);
                        // Force redirect to login page
                        window.location.replace('/login.html');
                    })
                    .catch(error => {
                        console.error("Logout error:", error);
                        // Even if there's an error, redirect to login page
                        window.location.replace('/login.html');
                    });
                });
                
                console.log("Logout button successfully replaced");
            } else {
                console.error("Could not find parent element for logout link");
            }
        } else {
            console.warn("Could not find logout link with selector 'li.nav-item a#logout-link'");
            
            // Try a broader selector
            const anyLogoutLink = document.querySelector('a#logout-link, a.logout, a[href*="logout"]');
            if (anyLogoutLink) {
                console.log("Found logout link with broader selector, adding direct event listener");
                
                // Add direct event listener
                anyLogoutLink.addEventListener('click', function(e) {
                    e.preventDefault();
                    console.log("Logout link clicked (broad selector)");
                    
                    // Clear session on client side
                    if (window.sessionStorage) {
                        sessionStorage.clear();
                    }
                    if (window.localStorage) {
                        localStorage.removeItem('user');
                        localStorage.removeItem('token');
                    }
                    
                    // Call the logout API endpoint
                    fetch('/api/logout', {
                        method: 'GET',
                        credentials: 'same-origin'
                    })
                    .then(response => {
                        console.log("Logout response:", response);
                        // Force redirect to login page
                        window.location.replace('/login.html');
                    })
                    .catch(error => {
                        console.error("Logout error:", error);
                        // Even if there's an error, redirect to login page
                        window.location.replace('/login.html');
                    });
                });
            } else {
                console.error("Could not find any logout link, adding global click handler");
                
                // Add global click handler for any element that might be a logout button
                document.addEventListener('click', function(e) {
                    const target = e.target;
                    
                    // Check if the clicked element or any of its parents has logout in the text, id, or class
                    let currentElement = target;
                    while (currentElement) {
                        // Check text content
                        if (currentElement.textContent && 
                            currentElement.textContent.toLowerCase().includes('logout')) {
                            
                            e.preventDefault();
                            console.log("Element with 'logout' text clicked");
                            
                            // Clear session and redirect
                            if (window.sessionStorage) {
                                sessionStorage.clear();
                            }
                            if (window.localStorage) {
                                localStorage.removeItem('user');
                                localStorage.removeItem('token');
                            }
                            
                            window.location.replace('/login.html');
                            break;
                        }
                        
                        // Check id and class
                        if ((currentElement.id && currentElement.id.toLowerCase().includes('logout')) ||
                            (currentElement.className && typeof currentElement.className === 'string' && 
                             currentElement.className.toLowerCase().includes('logout'))) {
                            
                            e.preventDefault();
                            console.log("Element with 'logout' in id or class clicked");
                            
                            // Clear session and redirect
                            if (window.sessionStorage) {
                                sessionStorage.clear();
                            }
                            if (window.localStorage) {
                                localStorage.removeItem('user');
                                localStorage.removeItem('token');
                            }
                            
                            window.location.replace('/login.html');
                            break;
                        }
                        
                        currentElement = currentElement.parentElement;
                    }
                });
            }
        }
    }
    
    // Run when DOM is loaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', fixLogoutButton);
    } else {
        // DOM is already loaded, run immediately
        fixLogoutButton();
    }
    
    // Also run after a short delay to ensure all other scripts have loaded
    setTimeout(fixLogoutButton, 1000);
})();
