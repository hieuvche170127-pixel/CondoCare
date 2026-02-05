// Profile page logic
console.log('Profile page loaded');

// Check login
if (!isLoggedIn()) {
    alert('Vui lòng đăng nhập');
    window.location.href = '/login';
}

const userInfo = getUserInfo();

// Load user info
if (userInfo) {
    document.getElementById('sidebarUserName').textContent = userInfo.fullName;
    
    // Load profile data
    loadProfileData();
}

// Logout button
document.getElementById('logoutBtn').addEventListener('click', function(e) {
    e.preventDefault();
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
        logout();
    }
});

/**
 * Load profile data
 */
async function loadProfileData() {
    showLoading();
    
    try {
        const response = await apiRequest(`${API_BASE_URL}/profile/me`);
        
        if (response.ok) {
            const data = await response.json();
            
            // Fill form
            document.getElementById('fullName').value = data.fullName || '';
            document.getElementById('email').value = data.email || '';
            document.getElementById('phone').value = data.phone || '';
            document.getElementById('position').value = data.position || data.type || '';
            document.getElementById('department').value = data.department || data.apartmentId || '';
        } else {
            showAlert('Không thể tải thông tin', 'danger');
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        showAlert('Có lỗi xảy ra', 'danger');
    } finally {
        hideLoading();
    }
}

/**
 * Update profile form submit
 */
document.getElementById('profileForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const formData = {
        fullName: document.getElementById('fullName').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value.trim()
    };
    
    showLoading();
    
    try {
        const response = await apiRequest(`${API_BASE_URL}/profile/update`, {
            method: 'PUT',
            body: JSON.stringify(formData)
        });
        
        const data = await response.text();
        
        if (response.ok) {
            // Update localStorage userInfo
            const currentUserInfo = getUserInfo();
            currentUserInfo.fullName = formData.fullName;
            currentUserInfo.email = formData.email;
            saveUserInfo(currentUserInfo);
            
            // Show success
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-success alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-check-circle"></i> <strong>Thành công!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').innerHTML = '';
            document.getElementById('alertContainer').appendChild(alertDiv);
            
            // Update sidebar name
            document.getElementById('sidebarUserName').textContent = formData.fullName;
        } else {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-exclamation-circle"></i> <strong>Lỗi!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').innerHTML = '';
            document.getElementById('alertContainer').appendChild(alertDiv);
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show';
        alertDiv.innerHTML = `
            <i class="fas fa-exclamation-triangle"></i> <strong>Lỗi!</strong> Có lỗi xảy ra
        `;
        document.getElementById('alertContainer').innerHTML = '';
        document.getElementById('alertContainer').appendChild(alertDiv);
    } finally {
        hideLoading();
    }
});

/**
 * Change password form submit
 */
document.getElementById('changePasswordForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmNewPassword = document.getElementById('confirmNewPassword').value;
    
    // Validation
    if (newPassword !== confirmNewPassword) {
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show';
        alertDiv.innerHTML = `
            <i class="fas fa-exclamation-circle"></i> Mật khẩu mới không khớp!
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.getElementById('alertContainer').innerHTML = '';
        document.getElementById('alertContainer').appendChild(alertDiv);
        return;
    }
    
    showLoading();
    
    try {
        const response = await apiRequest(`${API_BASE_URL}/profile/change-password`, {
            method: 'PUT',
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmNewPassword
            })
        });
        
        const data = await response.text();
        
        if (response.ok) {
            // Success
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-success alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-check-circle"></i> <strong>Thành công!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').innerHTML = '';
            document.getElementById('alertContainer').appendChild(alertDiv);
            
            // Reset form
            document.getElementById('changePasswordForm').reset();
        } else {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-exclamation-circle"></i> <strong>Lỗi!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').innerHTML = '';
            document.getElementById('alertContainer').appendChild(alertDiv);
        }
    } catch (error) {
        console.error('Error changing password:', error);
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show';
        alertDiv.innerHTML = `
            <i class="fas fa-exclamation-triangle"></i> <strong>Lỗi!</strong> Có lỗi xảy ra
        `;
        document.getElementById('alertContainer').innerHTML = '';
        document.getElementById('alertContainer').appendChild(alertDiv);
    } finally {
        hideLoading();
    }
});

/**
 * Show alert helper
 */
function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.getElementById('alertContainer').innerHTML = '';
    document.getElementById('alertContainer').appendChild(alertDiv);
}
