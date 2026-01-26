function login(event) {
    event.preventDefault();
    const formData = new FormData(document.getElementById('login-form'));
    fetch('/api/auth/login', {
        method: 'POST',
        body: formData
    }).then(response => {
        if (response.ok) {
            checkCurrentUser();  // Redirect based on role
        } else {
            document.getElementById('error-message').textContent = 'Invalid credentials or account blocked';
        }
    });
}

function checkCurrentUser() {
    fetch('/api/auth/current-user').then(response => {
        if (response.ok) {
            return response.json();
        } else {
            window.location.href = '/login.html';
        }
    }).then(user => {
        if (user.roleId === 'R_MGR' || user.roleId.startsWith('R_S') || user.roleId === 'R_ACC') {  // Staff groups
            window.location.href = '/admin/dashboard.html';
        } else if (user.roleId === 'R_RES') {
            window.location.href = '/resident/dashboard.html';  // Tạm, implement sau
        }
    });
}

function logout() {
    fetch('/api/auth/logout', { method: 'POST' }).then(() => {
        window.location.href = '/index.html';
    });
}

function checkAuth() {
    fetch('/api/auth/current-user').then(response => {
        if (!response.ok) {
            window.location.href = '/login.html';
        } else {
            return response.json();
        }
    }).then(user => {
        document.getElementById('user-name').textContent = user.fullName;
        // Show/hide based on role, e.g., in apartments.html
        if (window.location.pathname.includes('apartments.html')) {
            if (user.roleId === 'R_MGR') {
                document.getElementById('create-btn').style.display = 'block';
            }
        }
    }).catch(() => window.location.href = '/login.html');
}

document.getElementById('login-form')?.addEventListener('submit', login);