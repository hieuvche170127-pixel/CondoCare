/**
 * Common JavaScript functions cho Apartment Management System
 */

// API Base URL
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Hiển thị loading spinner
 */
function showLoading() {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.classList.add('show');
    }
}

/**
 * Ẩn loading spinner
 */
function hideLoading() {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.classList.remove('show');
    }
}

/**
 * Hiển thị alert message
 * @param {string} message - Nội dung thông báo
 * @param {string} type - Loại alert (success, danger, warning, info)
 */
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.role = 'alert';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    // Tìm container để insert alert
    const container = document.querySelector('.container') || document.querySelector('.container-fluid');
    if (container) {
        container.insertBefore(alertDiv, container.firstChild);
        
        // Tự động ẩn sau 5 giây
        setTimeout(() => {
            alertDiv.remove();
        }, 5000);
    }
}

/**
 * Lưu token.
 * @param {string} token - JWT token
 * @param {boolean} remember - true = ghi nhớ (localStorage), false = sessionStorage
 */
function saveToken(token, remember = false) {
    if (remember) {
        localStorage.setItem('token', token);
        sessionStorage.removeItem('token');
    } else {
        sessionStorage.setItem('token', token);
        localStorage.removeItem('token');  // xóa nếu lần trước đã remember
    }
}

/**
 * Lấy token — ưu tiên sessionStorage, fallback localStorage (remember me)
 * @returns {string|null} JWT token
 */
function getToken() {
    return sessionStorage.getItem('token') || localStorage.getItem('token');
}

/**
 * Xóa token khỏi cả hai storage
 */
function removeToken() {
    sessionStorage.removeItem('token');
    localStorage.removeItem('token');
}

/**
 * Lưu user info — đồng bộ với nơi lưu token
 * @param {Object} userInfo - Thông tin user
 * @param {boolean} remember - true = localStorage
 */
function saveUserInfo(userInfo, remember = false) {
    if (remember) {
        localStorage.setItem('userInfo', JSON.stringify(userInfo));
        sessionStorage.removeItem('userInfo');
    } else {
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
        localStorage.removeItem('userInfo');
    }
}

/**
 * Lấy user info — ưu tiên sessionStorage
 * @returns {Object|null} User info
 */
function getUserInfo() {
    const raw = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo');
    try { return raw ? JSON.parse(raw) : null; } catch { return null; }
}

/**
 * Xóa user info khỏi cả hai storage
 */
function removeUserInfo() {
    sessionStorage.removeItem('userInfo');
    localStorage.removeItem('userInfo');
}

/**
 * Kiểm tra user đã đăng nhập chưa
 * @returns {boolean} true nếu đã đăng nhập
 */
function isLoggedIn() {
    return getToken() !== null;
}

/**
 * Logout user
 */
function logout() {
    removeToken();
    removeUserInfo();
    window.location.href = '/login';
}

/**
 * Redirect đến trang dashboard nếu đã đăng nhập
 */
function redirectIfLoggedIn() {
    if (isLoggedIn()) {
        const userInfo = getUserInfo();
        if (userInfo && userInfo.userType === 'staff') {
            window.location.href = '/dashboard';
        }
    }
}

/**
 * Redirect đến trang login nếu chưa đăng nhập
 */
function redirectIfNotLoggedIn() {
    if (!isLoggedIn()) {
        window.location.href = '/login';
    }
}

/**
 * Make API request với JWT token
 * @param {string} url - API endpoint
 * @param {Object} options - Fetch options
 * @returns {Promise} Fetch promise
 */
async function apiRequest(url, options = {}) {
    const token = getToken();

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` })
        }
    };

    const mergedOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };

    try {
        const response = await fetch(url, mergedOptions);

        // Nếu 401 Unauthorized, logout
        if (response.status === 401) {
            logout();
            return;
        }

        return response;
    } catch (error) {
        console.error('API Request Error:', error);
        throw error;
    }
}

/**
 * Validate email format
 * @param {string} email - Email để validate
 * @returns {boolean} true nếu email hợp lệ
 */
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate phone number (10-11 chữ số)
 * @param {string} phone - Phone number
 * @returns {boolean} true nếu hợp lệ
 */
function isValidPhone(phone) {
    const phoneRegex = /^[0-9]{10,11}$/;
    return phoneRegex.test(phone);
}

/**
 * Format số tiền VND
 * @param {number} amount - Số tiền
 * @returns {string} Số tiền đã format
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

/**
 * Format date
 * @param {string} dateString - Date string
 * @returns {string} Formatted date
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN').format(date);
}

/**
 * Debounce function
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Event listener khi DOM đã load
document.addEventListener('DOMContentLoaded', function() {
    // Thêm logout button handler nếu có
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();
            logout();
        });
    }
});

/**
 * Lưu session sau khi login thành công.
 * Gọi hàm này trong login.html thay vì gọi saveToken/saveUserInfo riêng lẻ.
 * @param {string} token - JWT token từ server
 * @param {Object} userInfo - Thông tin user từ server
 * @param {boolean} remember - Giá trị checkbox "Ghi nhớ đăng nhập"
 */
function saveLoginSession(token, userInfo, remember = false) {
    saveToken(token, remember);
    saveUserInfo(userInfo, remember);
}