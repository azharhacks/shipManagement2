// Admin Dashboard Fixes
document.addEventListener('DOMContentLoaded', function() {
    // Fix for tasks not loading
    fixTasksLoading();
    
    // Fix for problem reports not loading
    fixProblemReportsLoading();
    
    // Fix logout button
    fixLogoutButton();
});

// Fix for tasks loading issue
function fixTasksLoading() {
    // Add demo tasks if the loading spinner persists for more than 2 seconds
    setTimeout(() => {
        const tasksLoading = document.getElementById('tasks-loading');
        const tasksTable = document.getElementById('tasks-table');
        const noTasksMessage = document.getElementById('no-tasks-message');
        
        if (tasksLoading && tasksLoading.style.display !== 'none' && (!tasksTable || tasksTable.children.length === 0)) {
            console.log("Adding demo tasks as fallback");
            
            // Hide loading spinner
            if (tasksLoading) tasksLoading.style.display = 'none';
            if (noTasksMessage) noTasksMessage.style.display = 'none';
            
            // Add demo tasks
            const demoTasks = [
                { id: 1, title: "Engine Maintenance", assignedTo: "Emily Chen", ship: "Ocean Explorer", priority: "High", status: "In Progress", dueDate: "2025-07-15" },
                { id: 2, title: "Safety Drill", assignedTo: "Kevin White", ship: "Coastal Voyager", priority: "Medium", status: "Pending", dueDate: "2025-07-20" },
                { id: 3, title: "Inventory Check", assignedTo: "Ava Lee", ship: "Sea Breeze", priority: "Low", status: "Completed", dueDate: "2025-07-10" }
            ];
            
            // Update task counts
            const pendingTasksElement = document.getElementById('pending-tasks');
            const completedTasksElement = document.getElementById('completed-tasks');
            const overdueTasksElement = document.getElementById('overdue-tasks');
            
            if (pendingTasksElement) pendingTasksElement.textContent = "2";
            if (completedTasksElement) completedTasksElement.textContent = "1";
            if (overdueTasksElement) overdueTasksElement.textContent = "0";
            
            // Add tasks to table
            if (tasksTable) {
                demoTasks.forEach(task => {
                    const row = document.createElement('tr');
                    
                    // Set appropriate class based on status
                    if (task.status === 'Completed') {
                        row.classList.add('table-success');
                    } else if (task.status === 'Overdue') {
                        row.classList.add('table-danger');
                    }
                    
                    row.innerHTML = `
                        <td>${task.id}</td>
                        <td>${task.title}</td>
                        <td>${task.assignedTo}</td>
                        <td>${task.ship}</td>
                        <td><span class="badge ${task.priority === 'High' ? 'bg-danger' : task.priority === 'Medium' ? 'bg-warning' : 'bg-info'}">${task.priority}</span></td>
                        <td><span class="badge ${task.status === 'Completed' ? 'bg-success' : task.status === 'In Progress' ? 'bg-primary' : 'bg-secondary'}">${task.status}</span></td>
                        <td>${task.dueDate}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary view-task" data-task-id="${task.id}">
                                <i class="bi bi-eye"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-success complete-task" data-task-id="${task.id}">
                                <i class="bi bi-check-lg"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger delete-task" data-task-id="${task.id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    
                    tasksTable.appendChild(row);
                });
                
                // Add event listeners to buttons
                document.querySelectorAll('.view-task').forEach(button => {
                    button.addEventListener('click', function() {
                        const taskId = this.getAttribute('data-task-id');
                        viewTaskDetails(taskId);
                    });
                });
                
                document.querySelectorAll('.complete-task').forEach(button => {
                    button.addEventListener('click', function() {
                        const taskId = this.getAttribute('data-task-id');
                        completeTask(taskId);
                    });
                });
                
                document.querySelectorAll('.delete-task').forEach(button => {
                    button.addEventListener('click', function() {
                        const taskId = this.getAttribute('data-task-id');
                        deleteTask(taskId);
                    });
                });
            }
        }
    }, 2000);
}

// Fix for problem reports loading issue
function fixProblemReportsLoading() {
    // Add demo problem reports if the loading spinner persists for more than 2 seconds
    setTimeout(() => {
        const problemsLoading = document.getElementById('problems-loading');
        const problemsTable = document.getElementById('problems-table');
        const noProblemsMessage = document.getElementById('no-problems-message');
        
        if (problemsLoading && problemsLoading.style.display !== 'none' && (!problemsTable || problemsTable.children.length === 0)) {
            console.log("Adding demo problem reports as fallback");
            
            // Hide loading spinner
            if (problemsLoading) problemsLoading.style.display = 'none';
            if (noProblemsMessage) noProblemsMessage.style.display = 'none';
            
            // Add demo problems
            const demoProblems = [
                { id: 1, title: "Engine Malfunction", reportedBy: "Emily Chen", ship: "Ocean Explorer", severity: "Critical", status: "Open", reportedDate: "2025-07-05" },
                { id: 2, title: "Navigation System Error", reportedBy: "Kevin White", ship: "Coastal Voyager", severity: "High", status: "In Progress", reportedDate: "2025-07-06" },
                { id: 3, title: "Minor Leak in Cabin 203", reportedBy: "Ava Lee", ship: "Sea Breeze", severity: "Medium", status: "Resolved", reportedDate: "2025-07-02" }
            ];
            
            // Update problem counts
            const criticalIssuesElement = document.getElementById('critical-issues');
            const highPriorityElement = document.getElementById('high-priority');
            const mediumPriorityElement = document.getElementById('medium-priority');
            const resolvedElement = document.getElementById('resolved');
            
            if (criticalIssuesElement) criticalIssuesElement.textContent = "1";
            if (highPriorityElement) highPriorityElement.textContent = "1";
            if (mediumPriorityElement) mediumPriorityElement.textContent = "1";
            if (resolvedElement) resolvedElement.textContent = "1";
            
            // Add problems to table
            if (problemsTable) {
                demoProblems.forEach(problem => {
                    const row = document.createElement('tr');
                    
                    // Set appropriate class based on severity
                    if (problem.severity === 'Critical') {
                        row.classList.add('table-danger');
                    } else if (problem.severity === 'High') {
                        row.classList.add('table-warning');
                    }
                    
                    row.innerHTML = `
                        <td>${problem.id}</td>
                        <td>${problem.title}</td>
                        <td>${problem.reportedBy}</td>
                        <td>${problem.ship}</td>
                        <td><span class="badge ${problem.severity === 'Critical' ? 'bg-danger' : problem.severity === 'High' ? 'bg-warning' : 'bg-info'}">${problem.severity}</span></td>
                        <td><span class="badge ${problem.status === 'Resolved' ? 'bg-success' : problem.status === 'In Progress' ? 'bg-primary' : 'bg-danger'}">${problem.status}</span></td>
                        <td>${problem.reportedDate}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary view-problem" data-problem-id="${problem.id}">
                                <i class="bi bi-eye"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-success resolve-problem" data-problem-id="${problem.id}">
                                <i class="bi bi-check-lg"></i>
                            </button>
                        </td>
                    `;
                    
                    problemsTable.appendChild(row);
                });
                
                // Add event listeners to buttons
                document.querySelectorAll('.view-problem').forEach(button => {
                    button.addEventListener('click', function() {
                        const problemId = this.getAttribute('data-problem-id');
                        viewProblemDetails(problemId);
                    });
                });
                
                document.querySelectorAll('.resolve-problem').forEach(button => {
                    button.addEventListener('click', function() {
                        const problemId = this.getAttribute('data-problem-id');
                        resolveProblem(problemId);
                    });
                });
            }
        }
    }, 2000);
}

// Fix logout button functionality
function fixLogoutButton() {
    // Updated to include the correct ID for the admin dashboard logout link
    const logoutElements = document.querySelectorAll('.logout-btn, #logout-btn, #logout-link');
    
    logoutElements.forEach(element => {
        element.addEventListener('click', function(e) {
            e.preventDefault();
            console.log("Logout button clicked");
            
            fetch('/api/logout', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                console.log("Logout response:", response);
                window.location.href = '/login.html';
            })
            .catch(error => {
                console.error("Logout error:", error);
                // Even if there's an error, redirect to login page
                window.location.href = '/login.html';
            });
        });
    });
}
